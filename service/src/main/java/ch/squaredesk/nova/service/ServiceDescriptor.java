/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service;

import java.util.Objects;

public class ServiceDescriptor {
    public final String serviceName;
    public final String instanceId;
    public final boolean lifecycleEnabled;

    public ServiceDescriptor() {
        this(null, null);
    }

    public ServiceDescriptor(String serviceName, String instanceId) {
        this(serviceName, instanceId, true);
    }

    public ServiceDescriptor(String serviceName, String instanceId, boolean lifecycleEnabled) {
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.lifecycleEnabled = lifecycleEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDescriptor that = (ServiceDescriptor) o;
        return lifecycleEnabled == that.lifecycleEnabled &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(instanceId, that.instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, instanceId, lifecycleEnabled);
    }

    @Override
    public String toString() {
        return serviceName + '.' + instanceId;
    }
}
