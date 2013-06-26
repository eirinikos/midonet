/*
 * Copyright 2012 Midokura PTE LTD.
 */
package org.midonet.midolman.state.zkManagers;

import org.midonet.midolman.serialization.Serializer;
import org.midonet.midolman.state.AbstractZkManager;
import org.midonet.midolman.state.Directory;
import org.midonet.midolman.state.PathBuilder;
import org.midonet.midolman.state.StateAccessException;
import org.midonet.midolman.state.ZkManager;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Zk DAO for tenants.  This class used purely by the REST API.
 */
public class TenantZkManager extends AbstractZkManager {

    private final static Logger log = LoggerFactory
            .getLogger(TenantZkManager.class);

    /**
     * Constructor to set ZooKeeper and base path.
     *
     * @param zk
     *         Zk data access class
     * @param paths
     *         PathBuilder class to construct ZK paths
     * @param serializer
     *         ZK data serialization class
     */
    public TenantZkManager(ZkManager zk, PathBuilder paths,
                           Serializer serializer) {
        super(zk, paths, serializer);
    }

    public TenantZkManager(Directory dir, String basePath,
                           Serializer serializer) {
        this(new ZkManager(dir), new PathBuilder(basePath), serializer);
    }

    public List<Op> prepareCreate(String tenantId) throws StateAccessException {

        List<Op> ops = new ArrayList<Op>();

        String tenantsPath = paths.getTenantsPath();
        if (!zk.exists(tenantsPath)) {
            ops.add(Op.create(tenantsPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT));
        }

        String tenantPath = paths.getTenantPath(tenantId);
        if (!zk.exists(tenantPath)) {
            ops.add(Op.create(tenantPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT));
        }

        return ops;
    }
}
