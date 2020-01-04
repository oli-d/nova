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

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.autoconfig.BeanIdentifiers;
import ch.squaredesk.nova.comm.http.autoconfig.HttpAdapterAutoConfig;
import ch.squaredesk.nova.comm.http.autoconfig.HttpAdapterMessageTranscriberAutoConfig;
import ch.squaredesk.nova.comm.http.autoconfig.HttpServerSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@AutoConfigureBefore(HttpAdapterAutoConfig.class)
@EnableConfigurationProperties({RestSettings.class, HttpServerSettings.class})
@Import(HttpAdapterMessageTranscriberAutoConfig.class)
public class RestAutoConfig {

    @Bean
    @ConditionalOnProperty(name = "nova.http.rest.log-invocations", havingValue = "true", matchIfMissing = true)
    RestInvocationLogger restInvocationLogger() {
        return new RestInvocationLogger();
    }

    @Bean
    @ConditionalOnMissingBean(name = BeanIdentifiers.SERVER)
    RestServerStarter restServerStarter(HttpServerSettings httpServerSettings,
                                        RestSettings restSettings,
                                        RestBeanPostprocessor restBeanPostprocessor,
                                        @Qualifier(BeanIdentifiers.OBJECT_MAPPER) @Autowired(required = false) ObjectMapper httpObjectMapper,
                                        Nova nova) {

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

        return new RestServerStarter(
                serverSettings,
                restSettings.getServerProperties(),
                restBeanPostprocessor,
                httpObjectMapper,
                restSettings.isCaptureMetrics(),
                nova);
    }

    /** We have to do everything HttpServer related here, so switch off the auto config */
    @Bean("theBigHttpServerInhibitor")
    public String theBigHttpServerInhibitor() {
        return "theBigHttpServerInhibitor";
    }

    @Bean
    RestBeanPostprocessor restBeanPostprocessor() {
        return new RestBeanPostprocessor();
    }

}
