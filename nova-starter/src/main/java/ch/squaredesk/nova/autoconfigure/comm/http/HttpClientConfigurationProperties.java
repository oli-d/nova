/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nova.http.client")
public class HttpClientConfigurationProperties {
    /** Defines, whether HTTP client mode should be enabled */
    private boolean enable = true;
    /** Default amount of seconds to wait for a reponse before a timeout is triggered */
    private int defaultRequestTimeoutInSeconds;
    /** Should zipping of transmitted data be enforced? */
    private boolean compressionEnforced;
    /** Amount of seconds to wait for a connection before a timeout is triggered */
    private int connectionTimeoutInSeconds;
    /** Amount of seconds to wait for a websocket connection before a timeout is triggered */
    private int webSocketTimeoutInSeconds;
    /** DANGER! If set to true, no HTTPS certificate validation is performed. Only use in DEV mode and at your own risk! */
    private boolean acceptAnyCertificate;
    private String userAgent;
    /** (Optional) SSL certificate. If this is set, the attribute sslCertificatePathIsIgnored */
    private String sslCertificateContent;
    /** (Optional) path to an SSL certificate */
    private String sslCertificatePath;
    /** (Optional) path to an SSL key store */
    private String sslKeyStorePath;
    /** (Optional) password of the SSL key store, defined in the attribute sslKeystorePath */
    private String sslKeyStorePass;

    public int getDefaultRequestTimeoutInSeconds() {
        return defaultRequestTimeoutInSeconds;
    }

    public void setDefaultRequestTimeoutInSeconds(int defaultRequestTimeoutInSeconds) {
        this.defaultRequestTimeoutInSeconds = defaultRequestTimeoutInSeconds;
    }

    public boolean isCompressionEnforced() {
        return compressionEnforced;
    }

    public void setCompressionEnforced(boolean compressionEnforced) {
        this.compressionEnforced = compressionEnforced;
    }

    public int getConnectionTimeoutInSeconds() {
        return connectionTimeoutInSeconds;
    }

    public void setConnectionTimeoutInSeconds(int connectionTimeoutInSeconds) {
        this.connectionTimeoutInSeconds = connectionTimeoutInSeconds;
    }

    public int getWebSocketTimeoutInSeconds() {
        return webSocketTimeoutInSeconds;
    }

    public void setWebSocketTimeoutInSeconds(int webSocketTimeoutInSeconds) {
        this.webSocketTimeoutInSeconds = webSocketTimeoutInSeconds;
    }

    public boolean isAcceptAnyCertificate() {
        return acceptAnyCertificate;
    }

    public void setAcceptAnyCertificate(boolean acceptAnyCertificate) {
        this.acceptAnyCertificate = acceptAnyCertificate;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSslCertificateContent() {
        return sslCertificateContent;
    }

    public void setSslCertificateContent(String sslCertificateContent) {
        this.sslCertificateContent = sslCertificateContent;
    }

    public String getSslKeyStorePath() {
        return sslKeyStorePath;
    }

    public void setSslKeyStorePath(String sslKeyStorePath) {
        this.sslKeyStorePath = sslKeyStorePath;
    }

    public String getSslKeyStorePass() {
        return sslKeyStorePass;
    }

    public void setSslKeyStorePass(String sslKeyStorePass) {
        this.sslKeyStorePass = sslKeyStorePass;
    }

    @Override
    public String toString() {
        return "HttpClientSettings{" +
                ", enabled='" + enable + '\'' +
                ", sslKeyStorePath='" + sslKeyStorePath + '\'' +
                ", sslKeyStorePass is " + (sslKeyStorePass == null ? "provided" : "null") +
                ", sslCertificateContent is " + (sslCertificateContent == null ? "provided" : "null") +
                ", defaultRequestTimeoutInSeconds='" + defaultRequestTimeoutInSeconds + '\'' +
                ", compressionEnforced='" + compressionEnforced + '\'' +
                ", connectionTimeoutInSeconds='" + connectionTimeoutInSeconds + '\'' +
                ", webSocketTimeoutInSeconds='" + webSocketTimeoutInSeconds + '\'' +
                ", sslAcceptAnyCertificate='" + acceptAnyCertificate + '\'' +
                ", userAgent='" + userAgent + '\'' +
                '}';
    }


    public String getSslCertificatePath() {
        return sslCertificatePath;
    }

    public void setSslCertificatePath(String sslCertificatePath) {
        this.sslCertificatePath = sslCertificatePath;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
