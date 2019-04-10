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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.metrics5.Timer;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.PreDestroy;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

public class RestServerStarter implements ApplicationListener<ContextRefreshedEvent> {
    private HttpServer httpServer;
    private HttpServerSettings httpServerSettings;
    private RestBeanPostprocessor restBeanPostprocessor;
    private ObjectMapper restObjectMapper;
    private boolean captureRestMetrics;
    private Nova nova;

    public RestServerStarter(HttpServerSettings httpServerSettings,
                             RestBeanPostprocessor restBeanPostprocessor,
                             ObjectMapper restObjectMapper,
                             boolean captureRestMetrics,
                             Nova nova) {
        this.httpServerSettings = httpServerSettings;
        this.restBeanPostprocessor = restBeanPostprocessor;
        this.restObjectMapper = restObjectMapper;
        this.captureRestMetrics = captureRestMetrics;
        this.nova = nova;
    }


    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (httpServer == null) {
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
            resourceConfig.registerInstances(restBeanPostprocessor.handlerBeans);
            // resourceConfig.packages(true, restPackagesToScanForHandlers);

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
            httpServer = GrizzlyHttpServerFactory.createHttpServer(serverAddress, resourceConfig, false);

            try {
                start();
            } catch (IOException e) {
                throw new BeanInitializationException("Unable to start REST server", e);
            }
        }
    }

    public void start() throws IOException {
        if (!httpServer.isStarted()) {
            httpServer.start();
        }
    }

    @PreDestroy
    public void shutdown() {
        if (httpServer != null) {
            httpServer.shutdown();
        }
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
