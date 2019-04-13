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
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static ch.squaredesk.nova.spring.NovaProvidingConfiguration.BeanIdentifiers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("medium")
class NovaProvidingConfigurationTest {
    private Nova createSut() {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext();
        ctx.register(MyEmptyConfig.class);
        ctx.refresh();
        return ctx.getBean(Nova.class);
    }

    @BeforeEach
    void clearEnvironment() {
        System.clearProperty(IDENTIFIER);
        System.clearProperty(WARN_ON_UNHANDLED_EVENTS);
        System.clearProperty(DEFAULT_BACKPRESSURE_STRATEGY);
        System.clearProperty(CAPTURE_VM_METRICS);
    }

    @Test
    void instanceIsCreatedWithDefaultValuesWhenJustImportingNovaProvidingConfig() {
        Nova sut = createSut();
        assertThat(sut.identifier, is(""));
        assertNotNull(sut.metrics.getMetrics().get(Metrics.name("jvm.mem")));
        assertNotNull(sut.metrics.getMetrics().get(Metrics.name("jvm.gc")));
        assertThat(sut.eventBus.eventBusConfig.warnOnUnhandledEvents, is(false));
        assertThat(sut.eventBus.eventBusConfig.defaultBackpressureStrategy, is(BackpressureStrategy.BUFFER));
    }

    @Test
    void identifierCanBeOverridenWithEnvironmentVariable() {
        System.setProperty(IDENTIFIER, "oli");
        Nova sut = createSut();
        assertThat(sut.identifier, is("oli"));
    }

    @Test
    void warnOnUnhandledEventCanBeOverridenWithEnvironmentVariable() {
        System.setProperty(WARN_ON_UNHANDLED_EVENTS, "true");
        Nova sut = createSut();
        assertThat(sut.eventBus.eventBusConfig.warnOnUnhandledEvents, is(true));
    }

    @Test
    void defaultBackpressureStrategyCanBeOverridenWithEnvironmentVariable() {
        System.setProperty(DEFAULT_BACKPRESSURE_STRATEGY, BackpressureStrategy.DROP.toString());
        Nova sut = createSut();
        assertThat(sut.eventBus.eventBusConfig.defaultBackpressureStrategy, is(BackpressureStrategy.DROP));
    }

    @Test
    void captureJvmMetricsCanBeOverridenWithEnvironmentVariable() {
        System.setProperty(CAPTURE_VM_METRICS, "false");
        Nova sut = createSut();
        assertNull(sut.metrics.getMetrics().get("jvm.mem"));
        assertNull(sut.metrics.getMetrics().get("jvm.gc"));
    }


    @Configuration
    @Import(NovaProvidingConfiguration.class)
    public static class MyEmptyConfig {
    }
}

