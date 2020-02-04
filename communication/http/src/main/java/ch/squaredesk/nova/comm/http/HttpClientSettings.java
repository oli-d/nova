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

package ch.squaredesk.nova.comm.http;


public class HttpClientSettings {
    public final boolean sslEnabled;
    public final int defaultRequestTimeoutInSeconds;
    public final boolean compressionEnforced;
    public final int connectionTimeoutInSeconds;
    public final int webSocketTimeoutInSeconds;
    public final boolean acceptAnyCertificate;
    public final String userAgent;
    public final String sslCertificateContent;
    public final String sslKeyStorePath;
    public final String sslKeyStorePass;

    private HttpClientSettings(Builder builder) {
        this.sslKeyStorePath = builder.sslKeyStorePath;
        this.sslKeyStorePass = builder.sslKeyStorePass;
        this.sslCertificateContent = builder.sslCertificateContent;
        this.defaultRequestTimeoutInSeconds = builder.defaultRequestTimeoutInSeconds;
        this.compressionEnforced = builder.compressionEnforced;
        this.connectionTimeoutInSeconds = builder.connectionTimeoutInSeconds;
        this.webSocketTimeoutInSeconds = builder.webSocketTimeoutInSeconds;
        this.acceptAnyCertificate = builder.acceptAnyCertificate;
        this.userAgent = builder.userAgent;
        this.sslEnabled = sslKeyStorePath != null || sslCertificateContent != null;
    }

    public static Builder builder() { return new Builder(); }

    @Override
    public String toString() {
        return "HttpClientSettings{" +
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

    public static class Builder {
        private int defaultRequestTimeoutInSeconds;
        private boolean compressionEnforced;
        private int connectionTimeoutInSeconds;
        private int webSocketTimeoutInSeconds;
        private boolean acceptAnyCertificate;
        private String userAgent;
        private String sslKeyStorePath;
        private String sslKeyStorePass;
        private String sslCertificateContent;

        public Builder defaultRequestTimeoutInSeconds(int defaultRequestTimeoutInSeconds) {
            this.defaultRequestTimeoutInSeconds = defaultRequestTimeoutInSeconds;
            return this;
        }

        public Builder compressionEnforced(boolean compressionEnforced) {
            this.compressionEnforced = compressionEnforced;
            return this;
        }

        public Builder connectionTimeoutInSeconds(int connectionTimeoutInSeconds) {
            this.connectionTimeoutInSeconds = connectionTimeoutInSeconds;
            return this;
        }

        public Builder webSocketTimeoutInSeconds(int webSocketTimeoutInSeconds) {
            this.webSocketTimeoutInSeconds = webSocketTimeoutInSeconds;
            return this;
        }

        public Builder sslAcceptAnyCertificate(boolean acceptAnyCertificate) {
            this.acceptAnyCertificate = acceptAnyCertificate;
            return this;
        }

        public Builder sslKeyStorePath(String s) {
            this.sslKeyStorePath = s;
            return this;
        }

        public Builder userAgent(String s) {
            this.userAgent = s;
            return this;
        }

        public Builder sslKeyStorePass(String s) {
            this.sslKeyStorePass = s;
            return this;
        }

        public Builder sslCertificateContent(String s) {
            this.sslCertificateContent = s;
            return this;
        }

        public HttpClientSettings build() {
            return new HttpClientSettings(this);
        }

    }
}
