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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashSet;
import java.util.Set;


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
    final Set<Object> handlerBeans = new HashSet<>();


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (BeanExaminer.isRestHandler(bean)) {
            handlerBeans.add(bean);
        }
        return bean;
    }

}
