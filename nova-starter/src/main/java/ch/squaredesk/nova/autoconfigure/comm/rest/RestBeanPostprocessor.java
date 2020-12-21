/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.rest;

import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashSet;
import java.util.Set;


/**
 * This class is used to register the properly annotated Rest endpoint classes.
 */
public class RestBeanPostprocessor implements BeanPostProcessor {
    final Set<Object> handlerBeans = new HashSet<>();


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (BeanExaminer.isRestHandler(bean)) {
            handlerBeans.add(bean);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

}
