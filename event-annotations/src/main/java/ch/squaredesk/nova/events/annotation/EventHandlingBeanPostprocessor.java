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

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.Timer;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class EventHandlingBeanPostprocessor implements BeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {
    private final Logger logger = LoggerFactory.getLogger(EventHandlingBeanPostprocessor.class);
    private final BeanExaminer beanExaminer = new BeanExaminer();

    final CopyOnWriteArrayList<EventHandlerDescription> handlerDescriptions = new CopyOnWriteArrayList<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        handlerDescriptions.addAll(Arrays.asList(beanExaminer.examine(bean)));
        return bean;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Nova nova = event.getApplicationContext().getBean(Nova.class);
        Objects.requireNonNull(nova,
                "Unable to initialize event handling, since no Nova instance was found in ApplicationContext");
        EventContext eventContext = new EventContext(nova.metrics, nova.eventBus);
        handlerDescriptions.forEach(hd -> registerEventHandler(hd, eventContext, nova.identifier));
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
                Timer timer = eventContext.metrics.getTimer(timerName);
                eventConsumer = new TimeMeasuringEventHandlingMethodInvoker(timer, invoker);
            }
            Flowable<Object[]> flowable = eventContext.eventBus.on(event, eventHandlerDescription.backpressureStrategy);
            if (eventHandlerDescription.dispatchOnBusinessLogicThread) {
                flowable = flowable.observeOn(NovaSchedulers.businessLogicThreadScheduler);
            }
            flowable.subscribe(eventConsumer);
        }
    }

    private static String prettyPrint(Object bean, Method method) {
        StringBuilder sb = new StringBuilder(bean.getClass().getName())
                .append('.')
                .append(method.getName())
                .append('(')
                .append(Arrays.stream(method.getParameterTypes())
                        .map(paramterClass -> paramterClass.getSimpleName())
                        .collect(Collectors.joining(", ")))
                .append(')');
        return sb.toString();
    }

}
