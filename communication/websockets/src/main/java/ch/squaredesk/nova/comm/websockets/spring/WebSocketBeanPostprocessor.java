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
import ch.squaredesk.nova.comm.websockets.server.ServerEndpoint;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpointFactory;
import io.reactivex.Flowable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;



public class WebSocketBeanPostprocessor implements BeanPostProcessor {
    private final MetricsCollector metricsCollector;
    private final ServerEndpointFactory serverEndpointFactory;
    private final BeanExaminer beanExaminer;

    public WebSocketBeanPostprocessor(MessageTranscriber<String> messageTranscriber, MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.serverEndpointFactory = new ServerEndpointFactory(messageTranscriber);
        beanExaminer = new BeanExaminer(messageTranscriber);
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        EndpointDescriptor[] endpoints = beanExaminer.websocketEndpointsIn(bean);
        for (EndpointDescriptor endpointDescriptor: endpoints) {
            ServerEndpoint se =
                serverEndpointFactory.createFor(
                        endpointDescriptor.destination,
                        endpointDescriptor.captureTimings ? metricsCollector : null);

            Flowable messages = se.messages(endpointDescriptor.messageType);
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
