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
import ch.squaredesk.nova.comm.http.spring.HttpServerProvidingConfiguration;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.net.URI;
import java.util.Arrays;

@Configuration
@Import({HttpServerProvidingConfiguration.class, NovaProvidingConfiguration.class})
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

    @Bean("restPackagesToScanForHandlers")
    public String[] packagesToScanForHandlers(Environment environment) {
        String value = System.getProperty("NOVA.REST.PACKAGES_TO_SCAN_FOR_HANDLERS", "");
        return value.split(",");
    }


    @Bean("httpServer")
    HttpServer httpServer(@Qualifier("restPackagesToScanForHandlers") String[] restPackagesToScanForHandlers,
                          @Qualifier("httpServerSettings") HttpServerSettings httpServerSettings,
                          @Qualifier("restObjectMapper") @Autowired(required = false) ObjectMapper restObjectMapper,
                          @Qualifier("captureRestMetrics") boolean captureRestMetrics,
                          Nova nova) {

        if (restPackagesToScanForHandlers == null ||
                Arrays.stream(restPackagesToScanForHandlers).allMatch(packageName -> packageName == null || packageName.trim().isEmpty())) {
            throw new RuntimeException("Unable to instantiate REST server with empty list of packages to scan for handlers");
        }

        ResourceConfig resourceConfig = new ResourceConfig()
                .register(MultiPartFeature.class)
                .register(JacksonFeature.class);

        // do we have a specific ObjectMapper?
        if (restObjectMapper != null) {
            // super ugly hack, but Jersey needs public static providers :-(
            SpecificRestObjectMapperProvider.STATIC_OBJECT_MAPPER = restObjectMapper;
        } else {
            SpecificRestObjectMapperProvider.STATIC_OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
        }
        resourceConfig.register(SpecificRestObjectMapperProvider.class);
        resourceConfig.packages(true, restPackagesToScanForHandlers);

        if (captureRestMetrics && nova != null) {
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

        URI serverAddress = UriBuilder.fromPath("http://" + httpServerSettings.interfaceName + ":" +
                httpServerSettings.port).build();
        return GrizzlyHttpServerFactory.createHttpServer(serverAddress, resourceConfig, false);
    }

    @Provider
    public static class SpecificRestObjectMapperProvider implements ContextResolver<ObjectMapper> {
        static ObjectMapper STATIC_OBJECT_MAPPER;

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return STATIC_OBJECT_MAPPER;
        }
    }
}
