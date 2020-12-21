/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties("nova.service")
public class NovaServiceConfigurationProperties {
    /** The name of the service. If not defined, the system tries to derive it from the service' class name. */
    private String serviceName;
    /** The ID of the current service instance. If not defined, a random UUID will be used */
    private String instanceId = UUID.randomUUID().toString();
    /** Should the service apply its own lifecycle events? This is normally NOT needed when using Spring, so it's disabled by default. */
    private boolean serviceLifecycleEnabled = false;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isServiceLifecycleEnabled() {
        return serviceLifecycleEnabled;
    }

    public void setServiceLifecycleEnabled(boolean serviceLifecycleEnabled) {
        this.serviceLifecycleEnabled = serviceLifecycleEnabled;
    }
}
