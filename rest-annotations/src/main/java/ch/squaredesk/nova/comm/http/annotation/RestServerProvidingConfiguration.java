/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http.annotation;

<<<<<<< HEAD
import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import com.codahale.metrics.Timer;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
=======
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
>>>>>>> admin-work
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

<<<<<<< HEAD
import javax.inject.Named;

@Configuration
@Import(RestEnablingConfiguration.class)
public class RestServerProvidingConfiguration {
    private Logger logger = LoggerFactory.getLogger(RestEnablingConfiguration.class);

=======
@Configuration
@Import(RestEnablingConfiguration.class)
public class RestServerProvidingConfiguration {
>>>>>>> admin-work
    @Autowired
    ResourceConfig resourceConfig;

    @Autowired
    HttpServerConfiguration serverConfig;

<<<<<<< HEAD
    @Autowired
    @Named("captureRestMetrics")
    boolean captureMetrics;

    @Autowired(required = false)
    Nova nova;

=======
>>>>>>> admin-work
    @Bean
    RestServerStarter restServerStarter() {
        return new RestServerStarter();
    }

    @Lazy // must be created after all other beans have been created (because of annotation processing)
    @Bean
    public HttpServer restHttpServer() {
<<<<<<< HEAD
        if (captureMetrics) {
            if (nova == null) {
                logger.warn("Metrics capturing switched on but no Nova instance found in application context. " +
                        "Metrics capturing therefore will NOT be performed!!!");
            } else {
                RequestEventListener requestEventListener = event -> {
                    String eventId = event.getContainerRequest().getPath(true);
                    Timer timer = nova.metrics.getTimer("rest", eventId);
                    if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
                        event.getContainerRequest().setProperty("metricsContext", timer.time());
                    } else if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_FINISHED) {
                        ((Timer.Context)event.getContainerRequest().getProperty("metricsContext")).stop();
                        nova.metrics.getCounter("rest", eventId, "total").inc();
                        if (event.getException()!=null) {
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
        }


=======
>>>>>>> admin-work
        return RestServerFactory.serverFor(serverConfig, resourceConfig);
    }

}
