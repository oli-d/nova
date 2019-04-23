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
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import ch.squaredesk.nova.comm.websockets.WebSocketAdapter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;


public class WebSocketBeanPostprocessor implements BeanPostProcessor {
    private final MetricsCollector metricsCollector;
    private final WebSocketAdapter webSocketAdapter;
    private final BeanExaminer beanExaminer;

    public WebSocketBeanPostprocessor(WebSocketAdapter webSocketAdapter,
                                      MessageTranscriber<String> messageTranscriber,
                                      MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.webSocketAdapter = webSocketAdapter;
        beanExaminer = new BeanExaminer(messageTranscriber);
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
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

}
