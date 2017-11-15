package ch.squaredesk.nova.comm.http;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

import java.util.Objects;

public class HttpServerFactory {
    public static HttpServer serverFor (HttpServerConfiguration serverConfig) {
        Objects.requireNonNull(serverConfig, "server config must not be null");

        HttpServer httpServer = new HttpServer();
        NetworkListener listener = new NetworkListener("root", serverConfig.interfaceName, serverConfig.port);

        if (serverConfig.isSslEnabled) {
            SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();
            sslContextConfigurator.setKeyStoreFile(serverConfig.sslKeyStorePath);
            if (serverConfig.sslKeyStorePass!=null)
            sslContextConfigurator.setKeyStorePass(serverConfig.sslKeyStorePass);
            sslContextConfigurator.setTrustStoreFile(serverConfig.sslTrustStorePath);
            if (serverConfig.sslTrustStorePass!=null)
            sslContextConfigurator.setTrustStorePass(serverConfig.sslTrustStorePass);
            boolean clientMode = false; // TODO
            boolean wantsClientAuth = false; // TODO
            SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(
                    sslContextConfigurator, clientMode, serverConfig.sslNeedsClientAuth, wantsClientAuth);
            listener.setSecure(true);
            listener.setSSLEngineConfig(sslEngineConfigurator);
        }

        httpServer.addListener(listener);
        return httpServer;
    }
}
