/*
 * Copyright (c) 2014 Midokura Europe SARL, All Rights Reserved.
 */

package org.midonet.client.dto;

import com.google.common.base.Objects;

import java.net.URI;
import java.util.UUID;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DtoPool {
    private UUID id;
    private URI uri;
    private String name;
    private String description;
    private UUID loadBalancerId;
    private URI loadBalancer;
    private UUID healthMonitorId;
    private URI healthMonitor;
    private URI vips;
    private URI poolMembers;
    private String protocol;
    private String lbMethod;
    private boolean adminStateUp = true;
    private LBStatus status = LBStatus.ACTIVE;
    private PoolHealthMonitorMappingStatus mappingStatus =
            PoolHealthMonitorMappingStatus.INACTIVE;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getLoadBalancerId() {
        return loadBalancerId;
    }

    public void setLoadBalancerId(UUID loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }

    public URI getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(URI loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public UUID getHealthMonitorId() {
        return healthMonitorId;
    }

    public void setHealthMonitorId(UUID healthMonitorId) {
        this.healthMonitorId = healthMonitorId;
    }

    public URI getHealthMonitor() {
        return healthMonitor;
    }

    public void setHealthMonitor(URI healthMonitor) {
        this.healthMonitor = healthMonitor;
    }

    public URI getVips() {
        return vips;
    }

    public void setVips(URI vips) {
        this.vips = vips;
    }

    public URI getPoolMembers() {
        return poolMembers;
    }

    public void setPoolMembers(URI poolMembers) {
        this.poolMembers = poolMembers;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getLbMethod() {
        return lbMethod;
    }

    public void setLbMethod(String lbMethod) {
        this.lbMethod = lbMethod;
    }

    public boolean isAdminStateUp() {
        return adminStateUp;
    }

    public void setAdminStateUp(boolean adminStateUp) {
        this.adminStateUp = adminStateUp;
    }

    public LBStatus getStatus() {
        return status;
    }

    public void setStatus(LBStatus status) {
        this.status = status;
    }

    public PoolHealthMonitorMappingStatus getMappingStatus() {
        return mappingStatus;
    }

    public void setMappingStatus(PoolHealthMonitorMappingStatus mappingStatus) {
        this.mappingStatus = mappingStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DtoPool that = (DtoPool) o;

        if (!Objects.equal(id, that.getId())) return false;
        if (!Objects.equal(name, that.getName())) return false;
        if (!Objects.equal(description, that.getDescription())) return false;
        if (!Objects.equal(loadBalancerId,
                that.getLoadBalancerId())) return false;
        if (!Objects.equal(healthMonitorId,
                that.getHealthMonitorId())) return false;
        if (!Objects.equal(protocol, that.getProtocol())) return false;
        if (!Objects.equal(lbMethod, that.getLbMethod())) return false;
        if (adminStateUp != that.isAdminStateUp()) return false;
        if (!Objects.equal(status, that.getStatus())) return false;
        if (!Objects.equal(mappingStatus,
                that.getMappingStatus())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result
                + (description != null ? description.hashCode() : 0);
        result = 31 * result
                + (loadBalancerId != null ? loadBalancerId.hashCode() : 0);
        result = 31 * result
                + (healthMonitorId != null ? healthMonitorId.hashCode() : 0);
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        result = 31 * result + (lbMethod != null ? lbMethod.hashCode() : 0);
        result = 31 * result + (adminStateUp ? 1 : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result
                + (mappingStatus != null ? mappingStatus.hashCode() : 0);
        return result;
    }
}
