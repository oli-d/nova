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
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.comm.http.spring.HttpEnablingConfiguration;
import ch.squaredesk.nova.comm.http.spring.HttpServerProvidingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@Import({HttpEnablingConfiguration.class})
public class RestEnablingConfiguration {
    public interface BeanIdentifiers {
        String CAPTURE_METRICS = "NOVA.REST.CAPTURE_METRICS";
        String REST_SERVER = "NOVA.REST.SERVER.INSTANCE";
    }

    @Bean(BeanIdentifiers.CAPTURE_METRICS)
    boolean captureRestMetrics(Environment environment) {
        boolean captureMetrics = true;
        try {
            captureMetrics = Boolean.valueOf(environment.getProperty(BeanIdentifiers.CAPTURE_METRICS, "true"));
        } catch (Exception e) {
            // noop, stick to default value
        }

        return captureMetrics;
    }

    @Bean
    RestServerStarter restServerStarter(@Qualifier(HttpServerProvidingConfiguration.BeanIdentifiers.SETTINGS) HttpServerSettings httpServerSettings,
                                        RestBeanPostprocessor restBeanPostprocessor,
                                        @Qualifier(HttpEnablingConfiguration.BeanIdentifiers.OBJECT_MAPPER) @Autowired(required = false) ObjectMapper httpObjectMapper,
                                        @Qualifier(BeanIdentifiers.CAPTURE_METRICS) boolean captureRestMetrics,
                                        Nova nova) {
        return new RestServerStarter(httpServerSettings, restBeanPostprocessor, httpObjectMapper, captureRestMetrics, nova);
    }

    @Bean
    RestBeanPostprocessor restBeanPostprocessor() {
        return new RestBeanPostprocessor();
    }

    /**
     * Switch off HttpServer auto creation. We can only do this after the ApplicationContext
     * is completely initialized, since we need all handler beans to be available.
     **/
    @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.AUTO_START_SERVER)
    public boolean autoStartHttpServer() {
        return false;
    }

    @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.AUTO_CREATE_SERVER)
    public boolean autoCreateHttpServer() {
        return false;
    }

    @Bean("autoNotifyAboutHttpServerAvailability")
    public boolean autoNotifyAboutHttpServerAvailability() {
        return false;
    }
}
