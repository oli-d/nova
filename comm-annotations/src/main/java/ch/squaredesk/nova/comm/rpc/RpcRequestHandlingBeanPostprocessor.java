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

package ch.squaredesk.nova.comm.rpc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Arrays;

public class RpcRequestHandlingBeanPostprocessor implements BeanPostProcessor {
    @Autowired
    RpcRequestProcessor rpcRequestProcessor;

    private final BeanExaminer beanExaminer = new BeanExaminer();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        RpcRequestHandlerDescription[] handlers = beanExaminer.examine(bean);
        Arrays.stream(handlers).forEach(handlerDescription -> {
            // TODO metrics
            rpcRequestProcessor.register(
                    handlerDescription.requestClass,
                    request -> handlerDescription.methodToInvoke.invoke(handlerDescription.bean, request));
        });
        return bean;
    }


}
