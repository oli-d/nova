/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.websockets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nova.ws")
public class WebSocketAdapterConfigurationProperties {
    private String adapterIdentifier;

    public String getAdapterIdentifier() {
        return adapterIdentifier;
    }

    public void setAdapterIdentifier(String adapterIdentifier) {
        this.adapterIdentifier = adapterIdentifier;
    }
}
