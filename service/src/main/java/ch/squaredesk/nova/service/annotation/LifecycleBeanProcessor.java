/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service.annotation;

import ch.squaredesk.nova.tuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


public class LifecycleBeanProcessor implements BeanPostProcessor {
    private final Logger logger = LoggerFactory.getLogger(LifecycleBeanProcessor.class);

    private final BeanExaminer beanExaminer = new BeanExaminer();

    private final CopyOnWriteArrayList<Pair<Object, Method[]>> initHandlers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Pair<Object, Method[]>> startupHandlers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Pair<Object, Method[]>> shutdownHandlers = new CopyOnWriteArrayList<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Method[] handlers = beanExaminer.initHandlersIn(bean);
        if (handlers.length > 0) {
            initHandlers.add(new Pair<>(bean, handlers));
        }
        handlers = beanExaminer.startupHandlersIn(bean);
        if (handlers.length > 0) {
            startupHandlers.add(new Pair<>(bean, handlers));
        }
        handlers = beanExaminer.shutdownHandlersIn(bean);
        if (handlers.length > 0) {
            shutdownHandlers.add(new Pair<>(bean, handlers));
        }
        return bean;
    }

    public void invokeInitHandlers() {
        invokeHandlers(initHandlers,
                t -> {
                    throw new BeanInitializationException("Error invoking init handler", t);
                } );
    }

    public void invokeStartupHandlers() {
        invokeHandlers(startupHandlers,
                t -> {
                    throw new RuntimeException("Error invoking startup handler", t);
                });
    }

    public void invokeShutdownHandlers() {
        invokeHandlers(shutdownHandlers,
                t -> logger.warn("Error invoking shutdown handler",t));
    }

    private void invokeHandlers(List<Pair<Object, Method[]>> handlers, Consumer<Throwable> exceptionHandler) {
        for (Pair<Object, Method[]> pair : handlers) {
            for (Method handlerMethod : pair._2) {
                try {
                    handlerMethod.invoke(pair._1);
                } catch (Throwable t) {
                    exceptionHandler.accept(t);
                }
            }
        }
    }
}
