/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events.annotation;

import ch.squaredesk.nova.events.EventBus;
import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.Timer;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;

public class EventHandlingBeanPostprocessor implements BeanPostProcessor {
    private final EventBus eventBus;
    private final String identifier;
    private final Metrics metrics;
    private final BeanExaminer beanExaminer = new BeanExaminer();


    public EventHandlingBeanPostprocessor(String identifier, EventBus eventEmitter, Metrics metrics) {
        this.eventBus = eventEmitter;
        this.identifier = identifier;
        this.metrics = metrics;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        beanExaminer.examine(bean, this::registerEventHandler);
        return bean;
    }

    private void registerEventHandler(String event,
                                      Object objectToInvokeMethodOn, Method methodToInvoke,
                                      BackpressureStrategy backpressureStrategy,
                                      boolean invokeOnBizLogicThread, boolean measureInvocationTime) {
        EventContext eventContext = new EventContext(metrics,eventBus);
        EventHandlingMethodInvoker invoker = new EventHandlingMethodInvoker(objectToInvokeMethodOn, methodToInvoke, eventContext);
        Consumer<Object[]> eventConsumer = invoker;
        if (measureInvocationTime) {
            String timerName = Metrics.name(identifier, "invocationTime", objectToInvokeMethodOn.getClass().getSimpleName(), event);
            Timer timer = metrics.getTimer(timerName);
            eventConsumer = new TimeMeasuringEventHandlingMethodInvoker(timer, invoker);
        }
        Flowable<Object[]> flowable = eventBus.on(event, backpressureStrategy);
        if (invokeOnBizLogicThread) {
            flowable = flowable.observeOn(NovaSchedulers.businessLogicThreadScheduler);
        }
        flowable.subscribe(eventConsumer);
    }
}
