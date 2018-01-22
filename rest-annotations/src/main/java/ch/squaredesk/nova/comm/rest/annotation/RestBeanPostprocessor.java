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

import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.PreDestroy;
import java.io.IOException;


/**
 * This class is used to register the properly annotated Rest endpoint classes. Since we implement
 * this as a BeanPostProcessor we are 1.) depending on Spring and 2.) this will only work for beans
 * known to Spring's ApplicationContext.
 *
 * An alternative would be to not rely on Spring at all and manually register the packages with
 * REST endpoints.
 *
 * Since we think that the first approach makes for a much nicer API, we bit the bullet and went for
 * the Spring dependency
 */
public class RestBeanPostprocessor implements BeanPostProcessor {
    final ResourceConfig resourceConfig = new ResourceConfig();


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        RestEndpoint[] restEndpoints = BeanExaminer.restEndpointsIn(bean);
        for (RestEndpoint endpointDescription: restEndpoints) {
            resourceConfig.registerResources(RestResourceFactory.resourceFor(endpointDescription.resourceDescriptor, bean, endpointDescription.handlerMethod));
        }
        return bean;
    }

}
