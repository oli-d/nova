/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service.admin;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class AdminCommandBeanPostprocessor implements BeanPostProcessor {
    private final AdminInfoCenter infoCenter;
    private final BeanExaminer beanExaminer = new BeanExaminer();


    public AdminCommandBeanPostprocessor(AdminInfoCenter infoCenter) {
        this.infoCenter = infoCenter;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        beanExaminer.examine(bean, infoCenter::register);
        return bean;
    }
}
