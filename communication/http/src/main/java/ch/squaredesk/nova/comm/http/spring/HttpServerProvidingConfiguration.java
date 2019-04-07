/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.comm.http.HttpServerFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
public class HttpServerProvidingConfiguration {

    @Bean("httpServerPort")
    int httpServerPort(Environment environment) {
        return environment.getProperty("NOVA.HTTP.SERVER.PORT", Integer.class, 10000);
    }

    @Bean("httpServerInterfaceName")
    String interfaceName(Environment environment) {
        return environment.getProperty("NOVA.HTTP.SERVER.INTERFACE_NAME", "0.0.0.0");
    }

    @Bean("httpServerTrustStore")
    String sslTrustStorePass(Environment environment) {
        return environment.getProperty("NOVA.HTTP.SERVER.TRUST_STORE");
    }

    @Bean("httpServerTrustStorePass")
    String sslTrustStorePath(Environment environment) {
        return environment.getProperty("NOVA.HTTP.SERVER.TRUST_STORE_PASS");
    }

    @Bean("httpServerKeyStore")
    String sslKeyStorePass(Environment environment) {
        return environment.getProperty("NOVA.HTTP.SERVER.KEY_STORE");
    }

    @Bean("httpServerKeyStorePass")
    String sslKeyStorePath(Environment environment) {
        return environment.getProperty("NOVA.HTTP.SERVER.KEY_STORE_PASS");
    }

    @Bean("httpServerSettings")
    HttpServerSettings httpServerSettings(
            @Qualifier("httpServerInterfaceName") String interfaceName,
            @Qualifier("httpServerPort") int httpServerPort,
            @Qualifier("httpServerTrustStore") @Autowired(required = false) String sslTrustStorePass,
            @Qualifier("httpServerTrustStorePass") @Autowired(required = false) String sslTrustStorePath,
            @Qualifier("httpServerKeyStore") @Autowired(required = false) String sslKeyStorePass,
            @Qualifier("httpServerKeyStorePass") @Autowired(required = false) String sslKeyStorePath) {
        return HttpServerSettings.builder()
                .interfaceName(interfaceName)
                .port(httpServerPort)
                .sslKeyStorePath(sslKeyStorePath)
                .sslKeyStorePass(sslKeyStorePass)
                .sslTrustStorePath(sslTrustStorePath)
                .sslTrustStorePass(sslTrustStorePass)
                .build();
    }

    @Bean("httpServer")
    HttpServer httpServer(@Qualifier("autoCreateHttpServer") boolean autoCreateHttpServer,
                          @Qualifier("httpServerSettings") @Autowired(required = false) HttpServerSettings httpServerSettings) {
        if (autoCreateHttpServer) {
            return HttpServerFactory.serverFor(httpServerSettings);
        } else {
            return null;
        }
    }

    @Bean("autoStartHttpServer")
    public boolean autoStartHttpServer(Environment environment) {
        return environment.getProperty("NOVA.HTTP.SERVER.AUTO_START", Boolean.class, true);
    }

    @Bean("autoCreateHttpServer")
    public boolean autoCreateHttpServer(Environment environment) {
        return environment.getProperty("NOVA.HTTP.SERVER.AUTO_CREATE", Boolean.class, true);
    }

    @Bean("httpServerStarter")
    public HttpServerStarter httpServerStarter(@Qualifier("autoStartHttpServer") boolean autoStartHttpServer,
                                               @Qualifier("httpServer") @Autowired(required = false) HttpServer httpServer) {
        return new HttpServerStarter(httpServer, autoStartHttpServer);
    }


}