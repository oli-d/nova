/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.spring;


import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.events.EventDispatchConfig;
import io.reactivex.BackpressureStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class NovaProvidingConfiguration {
    public interface BeanIdentifiers {
        String NOVA = "NOVA.INSTANCE";

        String SETTINGS = "NOVA.SETTINGS";
        String EVENT_DISPATCH_CONFIG = "NOVA.EVENTS.DISPATCH_CONFIG";
        String IDENTIFIER = "NOVA.ID";
        String DEFAULT_BACKPRESSURE_STRATEGY = "NOVA.EVENTS.DEFAULT_BACKPRESSURE_STRATEGY";
        String WARN_ON_UNHANDLED_EVENTS = "NOVA.EVENTS.WARN_ON_UNHANDLED";
        String DISPATCH_EVENTS_ON_SEPARATE_EXECUTOR = "NOVA.EVENTS.DISPATCH_ON_SEPARATE_EXECUTOR";
        String EVENT_DISPATCH_THREAD_POOL_SIZE = "NOVA.EVENTS.DISPATCH_THREAD_POOL_SIZE";;
        String CAPTURE_VM_METRICS = "NOVA.METRICS.CAPTURE_VM_METRICS";
    }

    @Bean(BeanIdentifiers.NOVA)
    public Nova nova(@Qualifier(BeanIdentifiers.SETTINGS) NovaSettings settings) {
        return Nova.builder()
                .setIdentifier(settings.identifier)
                .setEventDispatchConfig(settings.eventDispatchConfig)
                .captureJvmMetrics(settings.captureJvmMetrics)
                .build();
    }

    @Bean(BeanIdentifiers.EVENT_DISPATCH_CONFIG)
    public EventDispatchConfig eventBusConfig(
                                       @Qualifier(BeanIdentifiers.DEFAULT_BACKPRESSURE_STRATEGY) BackpressureStrategy defaultBackpressureStrategy,
                                       @Qualifier(BeanIdentifiers.WARN_ON_UNHANDLED_EVENTS) boolean warnOnUnhandledEvent,
                                       @Qualifier(BeanIdentifiers.DISPATCH_EVENTS_ON_SEPARATE_EXECUTOR) boolean dispatchEventsOnSeparateExecutor,
                                       @Qualifier(BeanIdentifiers.EVENT_DISPATCH_THREAD_POOL_SIZE) int eventDispatchThreadPoolSize) {
        return new EventDispatchConfig(defaultBackpressureStrategy, warnOnUnhandledEvent, dispatchEventsOnSeparateExecutor, eventDispatchThreadPoolSize);
    }

    @Bean(BeanIdentifiers.SETTINGS)
    public NovaSettings novaSettings(@Qualifier(BeanIdentifiers.IDENTIFIER) String identifier,
                     @Qualifier(BeanIdentifiers.EVENT_DISPATCH_CONFIG) EventDispatchConfig eventDispatchConfig,
                     @Qualifier(BeanIdentifiers.CAPTURE_VM_METRICS) boolean captureJvmMetrics) {
        return new NovaSettings(identifier, eventDispatchConfig, captureJvmMetrics);
    }

    @Bean(BeanIdentifiers.IDENTIFIER)
    public String identifier(Environment environment) {
        return environment.getProperty(BeanIdentifiers.IDENTIFIER, "");
    }

    @Bean(BeanIdentifiers.WARN_ON_UNHANDLED_EVENTS)
    public Boolean warnOnUnhandledEvent(Environment environment) {
        return environment.getProperty(BeanIdentifiers.WARN_ON_UNHANDLED_EVENTS, Boolean.class, false);
    }

    @Bean(BeanIdentifiers.DISPATCH_EVENTS_ON_SEPARATE_EXECUTOR)
    public Boolean dispatchEventsOnSeparateExecutor(Environment environment) {
        return environment.getProperty(BeanIdentifiers.DISPATCH_EVENTS_ON_SEPARATE_EXECUTOR, Boolean.class, false);
    }

    @Bean(BeanIdentifiers.EVENT_DISPATCH_THREAD_POOL_SIZE)
    public Integer eventDispatchThreadPoolSize(Environment environment) {
        return environment.getProperty(BeanIdentifiers.EVENT_DISPATCH_THREAD_POOL_SIZE, Integer.class, 1);
    }

    @Bean(BeanIdentifiers.CAPTURE_VM_METRICS)
    public Boolean captureJvmMetrics(Environment environment) {
        return environment.getProperty(BeanIdentifiers.CAPTURE_VM_METRICS, Boolean.class, true);
    }

    @Bean(BeanIdentifiers.DEFAULT_BACKPRESSURE_STRATEGY)
    public BackpressureStrategy defaultBackpressureStrategy(Environment environment) {
        String strategyAsString = environment.getProperty(
                BeanIdentifiers.DEFAULT_BACKPRESSURE_STRATEGY,
                String.class,
                BackpressureStrategy.BUFFER.toString());
        return BackpressureStrategy.valueOf(strategyAsString.toUpperCase());
    }

}
