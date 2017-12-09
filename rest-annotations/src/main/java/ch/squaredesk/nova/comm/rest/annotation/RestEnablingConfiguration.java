/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest.annotation;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.comm.http.spring.HttpServerConfigurationProvidingConfiguration;
import com.codahale.metrics.Timer;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Configuration
@Import(HttpServerConfigurationProvidingConfiguration.class)
@Order(value = Ordered.LOWEST_PRECEDENCE)
public class RestEnablingConfiguration {
    @Autowired
    Environment environment;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    Nova nova;
    @Autowired
    HttpServerConfiguration httpServerConfiguration;

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
    ch.squaredesk.nova.comm.rest.annotation.RestServerStarter restServerStarter() {
        return new RestServerStarter();
    }

    @Lazy // must be created after all other beans have been created (because of annotation processing)
    @Bean("httpServer")
    public HttpServer restHttpServer() {
        RestBeanPostprocessor restBeanPostprocessor = applicationContext.getBean(RestBeanPostprocessor.class);
        ResourceConfig resourceConfig = restBeanPostprocessor.resourceConfig;

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
}
