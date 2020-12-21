/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nova")
public class NovaConfigurationProperties {
    /** The identifier of the Nova instance */
    private String identifier;
    /** Should metrics be captured automatically? */
    private boolean captureJvmMetrics = true;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean isCaptureJvmMetrics() {
        return captureJvmMetrics;
    }

    public void setCaptureJvmMetrics(boolean captureJvmMetrics) {
        this.captureJvmMetrics = captureJvmMetrics;
    }
}
