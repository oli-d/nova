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

package ch.squaredesk.nova.comm.websockets.annotation;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.spring.HttpServerProvidingConfiguration;
import ch.squaredesk.nova.comm.http.spring.HttpServerStarter;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
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
    public MessageMarshaller<Integer, String> messageMarshaller() {
        return integer -> String.valueOf(integer);
    }

    @Bean
    public MessageUnmarshaller<String, Integer> messageUnmarshaller() {
        return string -> Integer.parseInt(string);
    }

    @Bean
    public WebSocketBeanPostprocessor webSocketBeanPostProcessor() {
        // enable WebSockets on httpServer
        // TODO: would be cool, if we could somehow find out whether this was already done
        WebSocketAddOn addon = new WebSocketAddOn();
        for (NetworkListener listener : httpServer.getListeners()) {
            listener.registerAddOn(addon);
        }

        return new WebSocketBeanPostprocessor(messageMarshaller(), messageUnmarshaller(), new MetricsCollector(nova.metrics));
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
