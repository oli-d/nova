/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;

import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

import java.util.Objects;

public class HttpServerFactory {
    private HttpServerFactory() {
    }

    public static HttpServer serverFor (HttpServerSettings serverConfig) {
        Objects.requireNonNull(serverConfig, "server config must not be null");

        HttpServer httpServer = new HttpServer();
        NetworkListener listener = new NetworkListener("root", serverConfig.interfaceName, serverConfig.port);

        if (serverConfig.isSslEnabled) {
            SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();
            sslContextConfigurator.setKeyStoreFile(serverConfig.sslKeyStorePath);
            if (serverConfig.sslKeyStorePass!=null) {
                sslContextConfigurator.setKeyStorePass(serverConfig.sslKeyStorePass);
            }
            sslContextConfigurator.setTrustStoreFile(serverConfig.sslTrustStorePath);
            if (serverConfig.sslTrustStorePass!=null) {
                sslContextConfigurator.setTrustStorePass(serverConfig.sslTrustStorePass);
            }
            boolean clientMode = false; // TODO
            boolean wantsClientAuth = serverConfig.sslNeedsClientAuth; // TODO
            SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(
                    sslContextConfigurator, clientMode, serverConfig.sslNeedsClientAuth, wantsClientAuth);
            listener.setSecure(true);
            listener.setSSLEngineConfig(sslEngineConfigurator);
        }

        if (serverConfig.compressData) {
            listener.getCompressionConfig().setCompressionMode(CompressionConfig.CompressionMode.ON);
            listener.getCompressionConfig().setCompressibleMimeTypes(serverConfig.compressibleMimeTypes);
        }

        httpServer.addListener(listener);
        return httpServer;
    }
}
