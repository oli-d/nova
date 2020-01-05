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

package ch.squaredesk.nova.comm.http.autoconfig;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("nova.http.server")
public class HttpServerConfigurationProperties {
    /** Defines, whether HTTP server mode should be enabled */
    private boolean enable = true;
    /** The interface, the HTTP server should listen to */
    private String interfaceName = "0.0.0.0";
    /** The port, the HTTP server should listen to */
    private int port = 10000;
    /** Defines, whether HTTPS is supposed to authenticate the client*/
    private boolean sslNeedsClientAuth = false;
    /** The path to the SSL key store that should be used for HTTPS communication */
    private String sslKeyStorePath;
    /** The password of the SSL key store that should be used for HTTPS communication */
    private String sslKeyStorePass;
    /** The path to the SSL trust store that should be used for HTTPS communication */
    private String sslTrustStorePath;
    /** The password of the SSL trust store that should be used for HTTPS communication */
    private String sslTrustStorePass;
    /** Should the server be automatically started? */
    private boolean autoStartServer = true;
    /** Should the instance creation should be published to registered listener? Only turn off, if you know what you're doing!!! */
    private boolean notifyAboutInstanceCreation = true;
    /** Should transmitted data be zipped? */
    private boolean compressData;
    /** Should transmitted data be zipped? */
    private List<String> compressibleMimeTypes = new ArrayList<>();

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public String getSslTrustStorePath() {
        return sslTrustStorePath;
    }

    public void setSslTrustStorePath(String sslTrustStorePath) {
        this.sslTrustStorePath = sslTrustStorePath;
    }

    public String getSslTrustStorePass() {
        return sslTrustStorePass;
    }

    public void setSslTrustStorePass(String sslTrustStorePass) {
        this.sslTrustStorePass = sslTrustStorePass;
    }

    public boolean isSslNeedsClientAuth() {
        return sslNeedsClientAuth;
    }

    public void setSslNeedsClientAuth(boolean sslNeedsClientAuth) {
        this.sslNeedsClientAuth = sslNeedsClientAuth;
    }

    public boolean isCompressData() {
        return compressData;
    }

    public void setCompressData(boolean compressData) {
        this.compressData = compressData;
    }

    public List<String> getCompressibleMimeTypes() {
        return compressibleMimeTypes;
    }

    public void setCompressibleMimeTypes(List<String> compressibleMimeTypes) {
        this.compressibleMimeTypes = compressibleMimeTypes;
    }

    @Override
    public String toString() {
        return "HttpServerSettings{" +
                "enabled='" + enable + '\'' +
                ", interfaceName='" + interfaceName + '\'' +
                ", port=" + port +
                ", sslKeyStorePath='" + sslKeyStorePath + '\'' +
                ", sslKeyStorePass='" + sslKeyStorePass + '\'' +
                ", sslTrustStorePath='" + sslTrustStorePath + '\'' +
                ", sslTrustStorePass='" + sslTrustStorePass + '\'' +
                ", sslNeedsClientAuth=" + sslNeedsClientAuth +
                '}';
    }

    public boolean isAutoStartServer() {
        return autoStartServer;
    }

    public void setAutoStartServer(boolean autoStartServer) {
        this.autoStartServer = autoStartServer;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isNotifyAboutInstanceCreation() {
        return notifyAboutInstanceCreation;
    }

    public void setNotifyAboutInstanceCreation(boolean notifyAboutInstanceCreation) {
        this.notifyAboutInstanceCreation = notifyAboutInstanceCreation;
    }
}
