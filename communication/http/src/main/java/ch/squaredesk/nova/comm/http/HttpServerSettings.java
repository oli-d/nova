/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HttpServerSettings {
    public final String interfaceName;
    public final int port;
    public final String sslKeyStorePath;
    public final String sslKeyStorePass;
    public final String sslTrustStorePath;
    public final String sslTrustStorePass;
    public final boolean sslNeedsClientAuth;
    public final boolean isSslEnabled;
    public final boolean compressData;
    public final Set<String> compressibleMimeTypes;

    private HttpServerSettings(Builder builder) {
        this.interfaceName = builder.interfaceName;
        this.port = builder.port;
        this.sslKeyStorePath = builder.sslKeyStorePath;
        this.sslKeyStorePass = builder.sslKeyStorePass;
        this.sslTrustStorePath = builder.sslTrustStorePath;
        this.sslTrustStorePass = builder.sslTrustStorePass;
        this.sslNeedsClientAuth = builder.sslNeedsClientAuth;
        this.isSslEnabled = sslKeyStorePath != null;
        this.compressData = builder.compressData;
        this.compressibleMimeTypes = builder.compressibleMimeTypes;
    }

    public static Builder builder() { return new Builder(); }

    @Override
    public String toString() {
        return "HttpServerSettings{" +
                "interfaceName='" + interfaceName + '\'' +
                ", port=" + port +
                ", sslKeyStorePath='" + sslKeyStorePath + '\'' +
                ", sslKeyStorePass='" + sslKeyStorePass + '\'' +
                ", sslTrustStorePath='" + sslTrustStorePath + '\'' +
                ", sslTrustStorePass='" + sslTrustStorePass + '\'' +
                ", sslNeedsClientAuth=" + sslNeedsClientAuth +
                '}';
    }

    public static class Builder {
        private boolean compressData;
        private Set<String> compressibleMimeTypes = new HashSet<>();
        private String interfaceName;
        private int port;
        private String sslKeyStorePath;
        private String sslKeyStorePass;
        private String sslTrustStorePath;
        private String sslTrustStorePass;
        private boolean sslNeedsClientAuth;

        public Builder() {
            compressibleMimeTypes.add("application/json");
        }

        public Builder interfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder sslKeyStorePath(String s) {
            this.sslKeyStorePath = s;
            return this;
        }

        public Builder sslKeyStorePass(String s) {
            this.sslKeyStorePass = s;
            return this;
        }

        public Builder sslTrustStorePath(String s) {
            this.sslTrustStorePath = s;
            return this;
        }

        public Builder sslTrustStorePass(String s) {
            this.sslTrustStorePass = s;
            return this;
        }

        public Builder compressData(boolean compress) {
            this.compressData = compress;
            return this;
        }

        public Builder sslNeedsClientAuth(boolean needsClientAuth) {
            this.sslNeedsClientAuth = needsClientAuth;
            return this;
        }

        public Builder addCompressibleMimeTypes(String ...compressibleMimeTypes) {
            if (compressibleMimeTypes != null) {
                this.compressibleMimeTypes.addAll(Arrays.asList(compressibleMimeTypes));
            }
            return this;
        }

        public HttpServerSettings build() {
            return new HttpServerSettings(this);
        }

    }
}
