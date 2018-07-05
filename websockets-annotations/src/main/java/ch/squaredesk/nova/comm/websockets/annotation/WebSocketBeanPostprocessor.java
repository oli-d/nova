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

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpoint;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpointFactory;
import io.reactivex.Flowable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;


public class WebSocketBeanPostprocessor implements BeanPostProcessor {
    private final MessageMarshaller messageMarshaller;
    private final MessageUnmarshaller messageUnmarshaller;
    private final MetricsCollector metricsCollector;
    private final ServerEndpointFactory serverEndpointFactory = new ServerEndpointFactory();

    public WebSocketBeanPostprocessor(MessageMarshaller<?, String> messageMarshaller, MessageUnmarshaller<String, ?> messageUnmarshaller, MetricsCollector metricsCollector) {
        this.messageMarshaller = messageMarshaller;
        this.messageUnmarshaller = messageUnmarshaller;
        this.metricsCollector = metricsCollector;
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        EndpointDescriptor[] endpoints = BeanExaminer.websocketEndpointsIn(bean);
        for (EndpointDescriptor endpointDescriptor: endpoints) {
            ServerEndpoint se =
                serverEndpointFactory.createFor(
                        endpointDescriptor.destination,
                        messageMarshaller,
                        messageUnmarshaller,
                        endpointDescriptor.captureTimings ? metricsCollector : null);

            Flowable messages = se.messages();
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
            messages.subscribe(MethodInvoker.createFor(endpointDescriptor, metricsCollector));
        }
        return bean;
    }

}
