from nose.plugins.attrib import attr
from mdts.lib.physical_topology_manager import PhysicalTopologyManager
from mdts.lib.virtual_topology_manager import VirtualTopologyManager
from mdts.lib.binding_manager import BindingManager
from mdts.tests.utils.asserts import *
from mdts.tests.utils import *

from hamcrest import *
from nose.tools import nottest

import logging
import subprocess

LOG = logging.getLogger(__name__)

PTM = PhysicalTopologyManager('../topologies/mmm_physical_test_vxlangw.yaml')
VTM = VirtualTopologyManager('../topologies/mmm_virtual_test_vxlangw.yaml')
BM = BindingManager(PTM, VTM)

binding1 = {
    'description': 'gateway on MM #1',
    'bindings': [
        {'binding':
             {'device_name': 'bridge-000-001', 'port_id': 2,
              'host_id': 1, 'interface_id': 1}},
        {'binding':
             {'device_name': 'router-000-001', 'port_id': 2,
              'host_id': 1, 'interface_id': 2}},
        ]
    }

# TODO(tomohiko) Move those to the virtual topology data file.
vtep_management_ip = '119.15.112.22'
vtep_management_port = '6633'
port_name = 'fakevm1_out'  # Physical port set up on the emulator.
vlan_id = 1234
vm_on_vtep = '10.0.1.1'

# An evil global variable:P
_vtep_binding = None


def setup():
    PTM.build()
    VTM.build()


def teardown():
    time.sleep(2)
    # Need to manually delete the _vtep_binding.
    # TODO(tomohiko) Remove once the wrapper classes are implemented.
    if _vtep_binding: _vtep_binding.delete()
    time.sleep(2)
    # Cannot do the proper cleanup at the moment because VTEP cannot be properly
    # deleted, which prevents the removal of the tunnel zone.
    #PTM.destroy()
    VTM.destroy()


@nottest
@bindings(binding1)
def test_internet_reachability():
    '''Tests a packet can go through to Internet.

    This does not test the VxLAN GW functionality. It is here as a utility
    during the development to test Internet reachability from a host. To be
    removed once the tests have been finalized.
    '''
    sender = BM.get_iface_for_port('bridge-000-001', 2)
    sender.execute('ping 8.8.8.8 -c2', sync=True)


@nottest
@bindings(binding1)
def test_ping_host_on_vtep():
    '''Tests if a VM can ping an IP address behind a VTEP.'''
    # Ping an IP address on the physical VTEP from a VM on a virtual bridge.
    LOG.debug('Ping to %s should not get through' % vm_on_vtep)
    sender = BM.get_iface_for_port('bridge-000-001', 2)

    pcap_filter = 'src host %s and icmp' % vm_on_vtep
#    f1 = sender.ping_ipv4_addr(vm_on_vtep, suppress_failure=True)
#    f2 = async_assert_that(sender,
#                           should_NOT_receive(pcap_filter, within_sec(5)))
#    wait_on_futures([f1, f2])

    # Sets up a VTEP and add a binding.
    set_up_vtep()

    # Send an ARP request.
    f1 = sender.send_arp_request(vm_on_vtep)
    wait_on_futures([f1])

    # Ping an IP address on the physical VTEP from a VM on a virtual bridge.
    f1 = sender.ping_ipv4_addr(vm_on_vtep)
    f2 = async_assert_that(sender, receives(pcap_filter, within_sec(5)))
    wait_on_futures([f1, f2])


def set_up_vtep():
    '''Helper function to set up a VTEP and a binding.

    Part of this setup should be declared in the virtual topology data, and be
    taken care of by VirtualTopologyManager, but the VTEP and VTEP binding
    wrappers for MDTS haven't been implemented yet, so they need to be set up by
    calling Python MidoNet Client directly here.

    TODO(tomohiko) Implement MDTS wrapper for VTEP and VTEP binding.
    '''
    LOG.debug('Setting up a VxLAN GW.')
    api = VTM._api
    # Look up the VTEP, or create one if it doesn't exist yet.
    vtep = None
    vteps = api.get_vteps()
    for v in vteps:
        if v.get_management_ip == vtep_management_ip:
            Log.debug('Found a VTP with mgmt IP=%s' % vtep_management_ip)
            vtep = v

    if not vtep:
        LOG.debug('Creating a new VTEP.')
        # Create a VTEP. Look up a tunnel zone from the host info.
        host_1 = None
        for h in PTM._hosts:
            host = h['host']
            if host.get('id') == 1: host_1 = host
        LOG.debug('Looked up the host id 1: %s' % host_1.get('mn_host_id'))

        LOG.debug('Look up a tunnel zone.')
        tz_data = host_1.get('tunnel_zone')
        tzs = api.get_tunnel_zones()
        tz = [t for t in tzs if t.get_name() == tz_data['name']]
        tunnel_zone = tz[0]
        tunnel_zone_id = tunnel_zone.get_id()
        LOG.debug('Tunnel zone name/IP/ID: %s/%s/%s' %
                  (tz_data['name'], tz_data['ip_addr'], tunnel_zone_id))

        vtep = api.add_vtep()\
                 .name('My VTEP')\
                 .management_ip(vtep_management_ip)\
                 .management_port(vtep_management_port)\
                 .tunnel_zone_id(tunnel_zone_id)\
                 .create()
        LOG.debug('Created a VTEP at %s' % vtep_management_ip)

    # Add a new VTEP binding. Assigning it to a global variable so that it can
    # be later deleted.
    # Look up a bridge with which to bind the VTEP.
    bridge = VTM.get_bridge('bridge-000-001')
    bridge_id = bridge._mn_resource.get_id()
    LOG.debug('Bridge ID: %s' % bridge_id)
    _vtep_binding = vtep.add_binding()\
                       .port_name(port_name)\
                       .vlan_id(vlan_id)\
                       .network_id(bridge_id)\
                       .create()
    LOG.debug('Added a binding')
