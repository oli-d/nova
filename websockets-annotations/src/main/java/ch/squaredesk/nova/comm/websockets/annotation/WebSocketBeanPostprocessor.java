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

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;


public class WebSocketBeanPostprocessor implements BeanPostProcessor {
    private final BeanExaminer beanExaminer = new BeanExaminer();

    final ResourceConfig resourceConfig = new ResourceConfig();


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        WebSocketEndpoint[] restEndpoints = beanExaminer.websocketEndpointsIn(bean);
        for (WebSocketEndpoint endpointDescription: restEndpoints) {
            // resourceConfig.registerResources(RestResourceFactory.resourceFor(endpointDescription.resourceDescriptor, bean, endpointDescription.handlerMethod));
        }
        return bean;
    }

}
