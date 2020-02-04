/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.HttpServerInstanceListener;
import ch.squaredesk.nova.metrics.Metrics;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Arrays;


public class WebSocketBeanProcessor implements ApplicationContextAware,
        HttpServerInstanceListener,
        ApplicationListener<ContextRefreshedEvent>,
        DisposableBean {
    private final WebSocketAdapter webSocketAdapter;
    private final String adapterIdentifier;
    private final BeanExaminer beanExaminer;
    private ApplicationContext applicationContext;
    private final Metrics metrics;
    private boolean wiredUp = false;

    public WebSocketBeanProcessor(WebSocketAdapter webSocketAdapter,
                                  MessageTranscriber<String> messageTranscriber,
                                  String adapterIdentifier,
                                  Metrics metrics) {
        this.webSocketAdapter = webSocketAdapter;
        this.adapterIdentifier = adapterIdentifier;
        this.metrics = metrics;
        beanExaminer = new BeanExaminer(messageTranscriber);
    }

    private void wireUpWebSocketHandlers() {
        if (wiredUp) {
            return;
        }

        wiredUp = true;

        Arrays.stream(applicationContext.getBeanDefinitionNames())
                .forEach(beanName -> processBean(applicationContext.getBean(beanName)));
    }

    public Object processBean(Object bean) {
        beanExaminer.onConnectHandlersIn(bean).forEach(handlerDescriptor ->
            webSocketAdapter
                .acceptConnections(handlerDescriptor.destination)
                .subscribe(ConnectEventHandlerMethodInvoker.createFor(handlerDescriptor, adapterIdentifier, metrics)));

        beanExaminer.onCloseHandlersIn(bean).forEach(handlerDescriptor ->
            webSocketAdapter
                .acceptConnections(handlerDescriptor.destination)
                .subscribe(webSocket -> webSocket.onClose(CloseEventHandlerMethodInvoker.createFor(handlerDescriptor, adapterIdentifier, metrics))));

        beanExaminer.onErrorHandlersIn(bean).forEach(handlerDescriptor ->
            webSocketAdapter
                .acceptConnections(handlerDescriptor.destination)
                .subscribe(webSocket -> webSocket.onError(ErrorEventHandlerMethodInvoker.createFor(handlerDescriptor, adapterIdentifier, metrics))));

        beanExaminer.onMessageEndpointsIn(bean).forEach(endpointDescriptor->
            webSocketAdapter
                .acceptConnections(endpointDescriptor.destination)
                .subscribe(webSocket -> webSocket.messages(endpointDescriptor.messageType).subscribe(
                        OnMessageMethodInvoker.createFor(endpointDescriptor, adapterIdentifier, metrics))));

        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!event.getApplicationContext().getBeansOfType(HttpServer.class).isEmpty()) {
            wireUpWebSocketHandlers();
        }
    }

    @Override
    public void httpServerInstanceCreated(HttpServer httpServer) {
        wireUpWebSocketHandlers();
    }

    @Override
    public void destroy() throws Exception {
        try {
            webSocketAdapter.shutdown();
        } catch (Exception e) {
            // noop; shutdown anyway...
        }
    }
}
