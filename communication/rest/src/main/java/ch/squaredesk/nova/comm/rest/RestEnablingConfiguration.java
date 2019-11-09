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
import ch.squaredesk.nova.tuples.Pair;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.Collection;
import java.util.Collections;

@Configuration
@Import({HttpEnablingConfiguration.class})
public class RestEnablingConfiguration {
    public interface BeanIdentifiers {
        String CAPTURE_METRICS = "NOVA.REST.CAPTURE_METRICS";
        String LOG_INVOCATIONS = "NOVA.REST.LOG_INVOCATIONS";
        String REST_SERVER_PROPERTIES = "NOVA.REST.SERVER.PROPERTIES";
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

    @Bean(BeanIdentifiers.LOG_INVOCATIONS)
    boolean logInvocations(Environment environment) {
        boolean logInvocations = true;
        try {
            logInvocations = Boolean.valueOf(environment.getProperty(BeanIdentifiers.LOG_INVOCATIONS, "true"));
        } catch (Exception e) {
            // noop, stick to default value
        }

        return logInvocations;
    }

    @Bean
    RestInvocationLogger restInvocationLogger(@Qualifier(BeanIdentifiers.LOG_INVOCATIONS) boolean logInvocations) {
        if (logInvocations) {
            return new RestInvocationLogger();
        } else {
            return null;
        }
    }

    @Bean(BeanIdentifiers.REST_SERVER_PROPERTIES)
    Collection<Pair<String, Object>> restServerProperties() {
        return Collections.emptyList();
    }

    @Bean
    RestServerStarter restServerStarter(@Qualifier(HttpServerProvidingConfiguration.BeanIdentifiers.SETTINGS) HttpServerSettings httpServerSettings,
                                        @Qualifier(BeanIdentifiers.REST_SERVER_PROPERTIES) Collection<Pair<String, Object>> restServerProperties,
                                        RestBeanPostprocessor restBeanPostprocessor,
                                        @Qualifier(HttpEnablingConfiguration.BeanIdentifiers.OBJECT_MAPPER) @Autowired(required = false) ObjectMapper httpObjectMapper,
                                        @Qualifier(BeanIdentifiers.CAPTURE_METRICS) boolean captureRestMetrics,
                                        Nova nova) {
        return new RestServerStarter(httpServerSettings, restServerProperties, restBeanPostprocessor, httpObjectMapper, captureRestMetrics, nova);
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
