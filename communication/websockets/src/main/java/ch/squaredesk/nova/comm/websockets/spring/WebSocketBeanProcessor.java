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

package ch.squaredesk.nova.comm.websockets.spring;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.spring.HttpEnablingConfiguration;
import ch.squaredesk.nova.comm.http.spring.HttpServerBeanListener;
import ch.squaredesk.nova.comm.http.spring.HttpServerProvidingConfiguration;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import ch.squaredesk.nova.comm.websockets.WebSocketAdapter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Arrays;


public class WebSocketBeanProcessor implements ApplicationContextAware, HttpServerBeanListener, ApplicationListener<ContextRefreshedEvent> {
    private final MetricsCollector metricsCollector;
    private final WebSocketAdapter webSocketAdapter;
    private final BeanExaminer beanExaminer;
    private ApplicationContext applicationContext;
    private boolean wiredUp = false;

    public WebSocketBeanProcessor(WebSocketAdapter webSocketAdapter,
                                  MessageTranscriber<String> messageTranscriber,
                                  MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.webSocketAdapter = webSocketAdapter;
        beanExaminer = new BeanExaminer(messageTranscriber);
    }

    private void wireUpWebSocketHandlers() {
        if (wiredUp) {
            return;
        }

        wiredUp = true;

        Arrays.stream(applicationContext.getBeanDefinitionNames())
                .forEach(beanName -> processBean(applicationContext.getBean(beanName), beanName));
    }

    public Object processBean(Object bean, String beanName) throws BeansException {
        beanExaminer.onConnectHandlersIn(bean).forEach(handlerDescriptor ->
            webSocketAdapter
                .acceptConnections(handlerDescriptor.destination)
                .subscribe(ConnectEventHandlerMethodInvoker.createFor(handlerDescriptor, metricsCollector)));

        beanExaminer.onCloseHandlersIn(bean).forEach(handlerDescriptor ->
            webSocketAdapter
                .acceptConnections(handlerDescriptor.destination)
                .subscribe(webSocket -> webSocket.onClose(CloseEventHandlerMethodInvoker.createFor(handlerDescriptor, metricsCollector))));

        beanExaminer.onErrorHandlersIn(bean).forEach(handlerDescriptor ->
            webSocketAdapter
                .acceptConnections(handlerDescriptor.destination)
                .subscribe(webSocket -> webSocket.onError(ErrorEventHandlerMethodInvoker.createFor(handlerDescriptor, metricsCollector))));

        beanExaminer.onMessageEndpointsIn(bean).forEach(endpointDescriptor->
            webSocketAdapter
                .acceptConnections(endpointDescriptor.destination)
                .subscribe(webSocket -> webSocket.messages(endpointDescriptor.messageType).subscribe(OnMessageMethodInvoker.createFor(endpointDescriptor, metricsCollector))));
                    /* FIXME: no Flowables at the moment
                    if (endpointDescriptor.backpressureStrategy!=null) {
                        switch (endpointDescriptor.backpressureStrategy) {
                            case BUFFER:
                                messages = messages.onBackpressureBuffer();
                                break;
                            case DROP:
                                messages = messages.onBackpressureDrop();
                                break;
                            case LATEST:
                                messages = messages.onBackpressureLatest();
                                break;
                        }
                    }
                    */

        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!event.getApplicationContext().getBeansOfType(HttpServer.class).isEmpty()) {
            wireUpWebSocketHandlers();
        }
    }

    @Override
    public void httpServerAvailableInContext(HttpServer httpServer) {
        wireUpWebSocketHandlers();
    }
}
