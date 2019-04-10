/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.comm.http.spring.HttpEnablingConfiguration;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@Import({HttpEnablingConfiguration.class, NovaProvidingConfiguration.class})
public class RestEnablingConfiguration {

    @Bean("captureRestMetrics")
    boolean captureRestMetrics(Environment environment) {
        boolean captureMetrics = true;
        try {
            captureMetrics = Boolean.valueOf(environment.getProperty("NOVA.REST.CAPTURE_METRICS", "true"));
        } catch (Exception e) {
            // noop, stick to default value
        }

        return captureMetrics;
    }

    @Bean
    RestServerStarter restServerStarter(@Qualifier("httpServerSettings") HttpServerSettings httpServerSettings,
                                        RestBeanPostprocessor restBeanPostprocessor,
                                        @Qualifier("restObjectMapper") @Autowired(required = false) ObjectMapper restObjectMapper,
                                        @Qualifier("captureRestMetrics") boolean captureRestMetrics,
                                        Nova nova) {
        return new RestServerStarter(httpServerSettings, restBeanPostprocessor, restObjectMapper, captureRestMetrics, nova);
    }

    @Bean
    RestBeanPostprocessor restBeanPostprocessor() {
        return new RestBeanPostprocessor();
    }

    /*
    @Bean("restPackagesToScanForHandlers")
    public String[] packagesToScanForHandlers(Environment environment) {
        String value = System.getProperty("NOVA.REST.PACKAGES_TO_SCAN_FOR_HANDLERS", "");
        return value.split(",");
    }
    */

    @Bean("httpMessageTranscriber")
    MessageTranscriber<String> httpMessageTranscriber(@Qualifier("restObjectMapper") @Autowired(required = false) ObjectMapper restObjectMapper) {
        if (restObjectMapper==null) {
            return new DefaultMessageTranscriberForStringAsTransportType();
        } else {
            return new MessageTranscriber<>(restObjectMapper::writeValueAsString, restObjectMapper::readValue);
        }
    }

    /**
     * Switch off HttpServer auto creation. We can only do this after the ApplicationContext
     * is completely initialized, since we need all handler beans to be available.
     *
     * For that reason, the HttpAdapter can only be used in client mode when using REST annotations.
     **/
    @Bean("autoStartHttpServer")
    public boolean autoStartHttpServer() {
        return false;
    }

    @Bean("autoCreateHttpServer")
    public boolean autoCreateHttpServer() {
        return false;
    }

}
