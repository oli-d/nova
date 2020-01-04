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

package ch.squaredesk.nova.comm.rest.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties("nova.http.rest")
public class RestSettings {
    private boolean logInvocations = true;
    private boolean captureMetrics = true;
    private Properties serverProperties;

    public boolean isLogInvocations() {
        return logInvocations;
    }

    public void setLogInvocations(boolean logInvocations) {
        this.logInvocations = logInvocations;
    }

    public boolean isCaptureMetrics() {
        return captureMetrics;
    }

    public void setCaptureMetrics(boolean captureMetrics) {
        this.captureMetrics = captureMetrics;
    }

    public Properties getServerProperties() {
        return serverProperties;
    }

    public void setServerProperties(Properties serverProperties) {
        this.serverProperties = serverProperties;
    }
}
