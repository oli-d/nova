/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.autoconfigure.comm.rest;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfigure.comm.http.BeanIdentifiers;
import ch.squaredesk.nova.autoconfigure.comm.http.HttpServerBeanNotifier;
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class RestServerStarter implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(RestServerStarter.class);

    private HttpServer httpServer;
    private HttpServerSettings httpServerSettings;
    private final Properties restServerProperties;
    private RestBeanPostprocessor restBeanPostprocessor;
    private ObjectMapper httpObjectMapper;
    private boolean captureRestMetrics;
    private Nova nova;

    public RestServerStarter(HttpServerSettings httpServerSettings,
                             Properties restServerProperties,
                             RestBeanPostprocessor restBeanPostprocessor,
                             ObjectMapper httpObjectMapper,
                             boolean captureRestMetrics,
                             Nova nova) {
        this.httpServerSettings = httpServerSettings;
        this.restServerProperties = Optional.ofNullable(restServerProperties).orElse(new Properties());
        this.restBeanPostprocessor = restBeanPostprocessor;
        this.httpObjectMapper = httpObjectMapper;
        this.captureRestMetrics = captureRestMetrics;
        this.nova = nova;
    }


    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (httpServer == null) {
            ResourceConfig resourceConfig = new ResourceConfig()
                    .register(MultiPartFeature.class)
                    .register(JacksonFeature.class)
                    // by default, we disable the WADL generation for security reasons
                    .property(ServerProperties.WADL_FEATURE_DISABLE, true);

            restServerProperties
                    .entrySet()
                    .stream()
                    .forEach(entry -> resourceConfig.property(String.valueOf(entry.getKey()), entry.getValue()));

            SSLEngineConfigurator sslEngineConfigurator = null;
            // FIXME: copy & paste of HttpServerFactory
            if (httpServerSettings.isSslEnabled) {
                SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();
                sslContextConfigurator.setKeyStoreFile(httpServerSettings.sslKeyStorePath);
                if (httpServerSettings.sslKeyStorePass != null) {
                    sslContextConfigurator.setKeyStorePass(httpServerSettings.sslKeyStorePass);
                }
                sslContextConfigurator.setTrustStoreFile(httpServerSettings.sslTrustStorePath);
                if (httpServerSettings.sslTrustStorePass != null) {
                    sslContextConfigurator.setTrustStorePass(httpServerSettings.sslTrustStorePass);
                }
                boolean clientMode = false; // TODO
                boolean wantsClientAuth = httpServerSettings.sslNeedsClientAuth; // TODO
                sslEngineConfigurator = new SSLEngineConfigurator(
                        sslContextConfigurator, clientMode, httpServerSettings.sslNeedsClientAuth, wantsClientAuth);
            }

            // do we have a specific ObjectMapper?
            if (httpObjectMapper != null) {
                // super ugly hack, but Jersey needs public static providers :-(
                SpecificRestObjectMapperProvider.STATIC_OBJECT_MAPPER = httpObjectMapper;
            } else {
                SpecificRestObjectMapperProvider.STATIC_OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            }
            resourceConfig.register(SpecificRestObjectMapperProvider.class);
            resourceConfig.registerInstances(restBeanPostprocessor.handlerBeans);

            if (captureRestMetrics && nova != null) {
                RequestEventListener requestEventListener = event -> {
                    String eventId = event.getContainerRequest().getPath(true).replace("/", ".");
                    if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
                        Timer timer = nova.metrics.getTimer("rest", eventId);
                        event.getContainerRequest().setProperty("metricsContext", timer.time());
                    } else if (event.getType() == RequestEvent.Type.RESP_FILTERS_START) {
                        Timer.Context timerContext = (Timer.Context) event.getContainerRequest().getProperty("metricsContext");
                        if (timerContext != null) {
                            timerContext.stop();
                            if (event.getException() != null) {
                                nova.metrics.getCounter("rest", eventId, "errors").inc();
                            }
                        }
                    }
                };
                resourceConfig.register(new ApplicationEventListener() {
                    @Override
                    public void onEvent(ApplicationEvent event) {
                        // nothing to do
                    }

                    @Override
                    public RequestEventListener onRequest(RequestEvent requestEvent) {
                        return requestEventListener;
                    }
                });
            }

            // register request filters and interceptors
            Map<String, ContainerRequestFilter> requestFilters =
                    contextRefreshedEvent.getApplicationContext().getBeansOfType(ContainerRequestFilter.class);
            requestFilters.values().forEach(filter -> {
                logger.debug("Registering request filter of type {}", filter.getClass().getName());
                resourceConfig.register(filter);
            });
            Map<String, ReaderInterceptor> readerInterceptors =
                    contextRefreshedEvent.getApplicationContext().getBeansOfType(ReaderInterceptor.class);
            readerInterceptors.values().forEach(filter -> {
                logger.debug("Registering reader interceptor of type {}", filter.getClass().getName());
                resourceConfig.register(filter);
            });
            Map<String, WriterInterceptor> writerInterceptors =
                    contextRefreshedEvent.getApplicationContext().getBeansOfType(WriterInterceptor.class);
            writerInterceptors.values().forEach(filter -> {
                logger.debug("Registering writer interceptor of type {}", filter.getClass().getName());
                resourceConfig.register(filter);
            });
            Map<String, ContainerResponseFilter> responseFilters =
                    contextRefreshedEvent.getApplicationContext().getBeansOfType(ContainerResponseFilter.class);
            responseFilters.values().forEach(filter -> {
                logger.debug("Registering response filter of type {}", filter.getClass().getName());
                resourceConfig.register(filter);
            });

            // register default error page generator
            Map <String, ErrorPageGenerator> defaultErrorPageGenerators =
                    contextRefreshedEvent.getApplicationContext().getBeansOfType(ErrorPageGenerator.class);
            if (defaultErrorPageGenerators.size() > 1) {
                throw new BeanInitializationException("Unable to create and start REST server, since more than one DefaultErrorPageGenerator beans were found: " +
                        defaultErrorPageGenerators.values().stream().map(bean -> bean.getClass().getSimpleName()).collect(Collectors.joining(",")));
            }
            URI serverAddress = UriBuilder.fromPath(
                    "http://" + httpServerSettings.interfaceName + ":" +
                    httpServerSettings.port).build();
            httpServer = GrizzlyHttpServerFactory.createHttpServer(
                    serverAddress,
                    resourceConfig,
                    sslEngineConfigurator != null,
                    sslEngineConfigurator,
                    false);

            defaultErrorPageGenerators.values().forEach(errorPageGenerator -> {
                logger.debug("Registering default error page generator {}", errorPageGenerator.getClass().getName());
                httpServer.getServerConfiguration().setDefaultErrorPageGenerator(errorPageGenerator);
            });

            // register server in ApplicationContext
            GenericApplicationContext genericContext = (GenericApplicationContext) contextRefreshedEvent.getApplicationContext();
            genericContext.registerBean(BeanIdentifiers.SERVER, HttpServer.class, () -> httpServer);

            // notify everyone interested
            HttpServerBeanNotifier.notifyHttpServerAvailableInContext(httpServer, contextRefreshedEvent.getApplicationContext());

            // and start the server
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

    @Override
    public void destroy() {
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
