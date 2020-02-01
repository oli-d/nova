/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.service;

public class NovaServiceConfig {
    public final String serviceName;
    public final String instanceId;
    public final boolean registerShutdownHook;

    public NovaServiceConfig(String serviceName, String instanceId, boolean registerShutdownHook) {
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.registerShutdownHook = registerShutdownHook;
    }
}
