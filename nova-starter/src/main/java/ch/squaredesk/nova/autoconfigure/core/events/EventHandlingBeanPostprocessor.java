/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.core.events;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.events.EventContext;
import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.Timer;
import io.reactivex.rxjava3.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class EventHandlingBeanPostprocessor implements BeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {
    private final Logger logger = LoggerFactory.getLogger(EventHandlingBeanPostprocessor.class);
    private final BeanExaminer beanExaminer = new BeanExaminer();

    private final EventContext eventContext;
    private final String novaIdentifier;


    final CopyOnWriteArrayList<EventHandlerDescription> handlerDescriptions = new CopyOnWriteArrayList<>();

    public EventHandlingBeanPostprocessor(Nova nova) {
        this.eventContext = new EventContext(nova.metrics, nova.eventBus);
        this.novaIdentifier = nova.identifier;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        handlerDescriptions.addAll(Arrays.asList(beanExaminer.examine(bean)));
        return bean;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        handlerDescriptions.forEach(hd -> registerEventHandler(hd, eventContext, novaIdentifier));
    }

    private void registerEventHandler(EventHandlerDescription eventHandlerDescription, EventContext eventContext, String novaIdentifier) {
        EventHandlingMethodInvoker invoker = new EventHandlingMethodInvoker(
                eventHandlerDescription.bean, eventHandlerDescription.methodToInvoke, eventContext);
        Consumer<Object[]> eventConsumer = invoker;
        for (String event: eventHandlerDescription.events) {
            logger.debug("Registering annotated event handler: {} -> {}",
                    event, prettyPrint(eventHandlerDescription.bean, eventHandlerDescription.methodToInvoke));
            if (eventHandlerDescription.captureInvocationTimeMetrics) {
                String timerName = Metrics.name(novaIdentifier, "invocationTime",
                        eventHandlerDescription.bean.getClass().getSimpleName(), event);
                Timer timer = eventContext.metrics().getTimer(timerName);
                eventConsumer = new TimeMeasuringEventHandlingMethodInvoker(timer, invoker);
            }
            eventContext
                    .eventBus().on(event, eventHandlerDescription.backpressureStrategy)
                    .subscribe(eventConsumer);
        }
    }

    private static String prettyPrint(Object bean, Method method) {
        StringBuilder sb = new StringBuilder(bean.getClass().getName())
                .append('.')
                .append(method.getName())
                .append('(')
                .append(Arrays.stream(method.getParameterTypes())
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", ")))
                .append(')');
        return sb.toString();
    }

}
