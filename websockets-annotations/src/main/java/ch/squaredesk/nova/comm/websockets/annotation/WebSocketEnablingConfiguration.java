/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.websockets.annotation;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.spring.HttpServerProvidingConfiguration;
import ch.squaredesk.nova.comm.http.spring.HttpServerStarter;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@Import(HttpServerProvidingConfiguration.class)
public class WebSocketEnablingConfiguration {
    @Autowired
    Environment environment;
    @Autowired
    Nova nova;
    @Autowired
    HttpServer httpServer;

    @Bean
    public MessageMarshaller messageMarshaller() {
        return string -> string;
    }

    @Bean
    public MessageUnmarshaller messageUnmarshaller() {
        return string -> string;
    }

    @Bean
    public WebSocketBeanPostprocessor webSocketBeanPostProcessor() {
        return new WebSocketBeanPostprocessor(messageMarshaller(), messageUnmarshaller(), new MetricsCollector(nova.metrics));
    }

    @Bean(name = "captureWebSocketMetrics")
    public boolean captureWebSocketMetrics() {
        boolean captureMetrics = true;
        try {
            captureMetrics = Boolean.valueOf(environment.getProperty("NOVA.HTTP.WEB_SOCKETS.CAPTURE_METRICS", "true"));
        } catch (Exception e) {
            // noop, stick to default value
        }

        return captureMetrics;
    }

    @Bean
    AsyncHttpClient httpClient() {
        AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder()
                // .setProxyServer(new ProxyServer("127.0.0.1", 38080))
                .build();
        return new AsyncHttpClient(cf);
    }

    @Bean
    HttpServerStarter httpServerStarter() {
        return new HttpServerStarter();
    }

}
