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

import ch.squaredesk.nova.comm.http.HttpServerFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HttpServerSettings.class)
@ConditionalOnMissingBean(name = "theBigHttpServerInhibitor")
public class HttpServerAutoConfig {
    @Bean(BeanIdentifiers.SERVER)
    HttpServer httpServer(HttpServerSettings httpServerSettings) {
        ch.squaredesk.nova.comm.http.HttpServerSettings serverSettings =
                ch.squaredesk.nova.comm.http.HttpServerSettings.builder()
                .port(httpServerSettings.getPort())
                .compressData(httpServerSettings.isCompressData())
                .sslNeedsClientAuth(httpServerSettings.isSslNeedsClientAuth())
                .sslKeyStorePass(httpServerSettings.getSslKeyStorePass())
                .sslKeyStorePath(httpServerSettings.getSslKeyStorePath())
                .sslTrustStorePass(httpServerSettings.getSslTrustStorePass())
                .sslTrustStorePath(httpServerSettings.getSslTrustStorePath())
                .interfaceName(httpServerSettings.getInterfaceName())
                .addCompressibleMimeTypes(httpServerSettings.getCompressibleMimeTypes().toArray(new String[0]))
                .build();
        return HttpServerFactory.serverFor(serverSettings);
    }

    @Bean
    @ConditionalOnBean(HttpServer.class)
    @ConditionalOnMissingBean(HttpServerStarter.class)
    @ConditionalOnProperty(name = "nova.http.server.auto-start-server", havingValue = "true", matchIfMissing = true)
    HttpServerStarter httpServerStarter(HttpServer httpServer) {
        return new HttpServerStarter(httpServer);
    }

    @Bean
    @ConditionalOnMissingBean(HttpServerBeanNotifier.class)
    // FIXME: delete property
    @ConditionalOnProperty(name = "nova.http.server.notify-about-instance-creation", havingValue = "true", matchIfMissing = true)
    public HttpServerBeanNotifier httpServerBeanNotifier() {
        return new HttpServerBeanNotifier();
    }
}