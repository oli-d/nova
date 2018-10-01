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
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.comm.http.spring.HttpServerConfigurationProvidingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.metrics5.Timer;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.net.URI;

@Configuration
@Import(HttpServerConfigurationProvidingConfiguration.class)
@Order // default = Ordered.LOWEST_PRECEDENCE, which is exactly what we want
public class RestEnablingConfiguration {
    @Autowired
    Environment environment;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    Nova nova;
    @Autowired
    HttpServerConfiguration httpServerConfiguration;
    @Autowired(required = false)
    ObjectMapper restObjectMapper;

    @Bean
    public static RestBeanPostprocessor restBeanPostProcessor() {
        return new RestBeanPostprocessor();
    }

    @Bean(name = "captureRestMetrics")
    public boolean captureRestMetrics() {
        boolean captureMetrics = true;
        try {
            captureMetrics = Boolean.valueOf(environment.getProperty("NOVA.HTTP.REST.CAPTURE_METRICS", "true"));
        } catch (Exception e) {
            // noop, stick to default value
        }

        return captureMetrics;
    }


    @Bean
    @Conditional(HttpServerAutostartCondition.class)
    RestServerStarter restServerStarter() {
        return new RestServerStarter();
    }

    @Lazy // must be created after all other beans have been created (because of annotation processing)
    @Bean("httpServer")
    public HttpServer restHttpServer() {
        RestBeanPostprocessor restBeanPostprocessor = applicationContext.getBean(RestBeanPostprocessor.class);
        ResourceConfig resourceConfig = new ResourceConfig()
            .register(MultiPartFeature.class)
            .register(JacksonFeature.class);

        // do we have a specific ObjectMapper?
        if (restObjectMapper!=null) {
            // super ugly hack, but Jersey needs public static providers :-(
            SpecificRestObjectMapperProvider.STATIC_OBJECT_MAPPER = restObjectMapper;
        } else {
            SpecificRestObjectMapperProvider.STATIC_OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
        }
        resourceConfig.register(SpecificRestObjectMapperProvider.class);
        resourceConfig.registerInstances(restBeanPostprocessor.handlerBeans.toArray());

        if (captureRestMetrics() && nova != null) {
            RequestEventListener requestEventListener = event -> {
                String eventId = event.getContainerRequest().getPath(true);
                Timer timer = nova.metrics.getTimer("rest", eventId);
                if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
                    event.getContainerRequest().setProperty("metricsContext", timer.time());
                } else if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_FINISHED) {
                    ((Timer.Context) event.getContainerRequest().getProperty("metricsContext")).stop();
                    nova.metrics.getCounter("rest", eventId, "total").inc();
                    if (event.getException() != null) {
                        nova.metrics.getCounter("rest", eventId, "errors").inc();
                    }
                }
            };

            resourceConfig.register(new ApplicationEventListener() {
                @Override
                public void onEvent(ApplicationEvent event) {
                }

                @Override
                public RequestEventListener onRequest(RequestEvent requestEvent) {
                    return requestEventListener;
                }
            });
        }

        URI serverAddress = UriBuilder.fromPath("http://" + httpServerConfiguration.interfaceName + ":" +
                httpServerConfiguration.port).build();
        return GrizzlyHttpServerFactory.createHttpServer(serverAddress, resourceConfig, false);
    }

    @Provider
    public static class SpecificRestObjectMapperProvider implements ContextResolver<ObjectMapper> {
        public static ObjectMapper STATIC_OBJECT_MAPPER;

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return STATIC_OBJECT_MAPPER;
        }
    }


}
