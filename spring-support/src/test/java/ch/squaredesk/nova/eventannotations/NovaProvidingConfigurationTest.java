package ch.squaredesk.nova.eventannotations;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.events.EventBusConfig;
import ch.squaredesk.nova.metrics.GarbageCollectionMeter;
import ch.squaredesk.nova.metrics.MemoryMeter;
import io.reactivex.BackpressureStrategy;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
        System.clearProperty("NOVA.ID");
        System.clearProperty("NOVA.EVENTS.WARN_ON_UNHANDLED");
        System.clearProperty("NOVA.EVENTS.BACKPRESSURE_STRATEGY");
        System.clearProperty("NOVA.METRICS.CAPTURE_VM_METRICS");
    }

    @Test
    void instanceIsCreatedWithDefaultValuesWhenJustImportingNovaProvidingConfig() {
        Nova sut = createSut();
        assertThat(sut.identifier, is(""));
        assertNotNull(sut.metrics.getMetrics().get("jvm.mem"));
        assertNotNull(sut.metrics.getMetrics().get("jvm.gc"));
        assertThat(sut.eventBus.eventBusConfig.warnOnUnhandledEvents, is(false));
        assertThat(sut.eventBus.eventBusConfig.defaultBackpressureStrategy, is(BackpressureStrategy.BUFFER));
    }

    @Test
    void identifierCanBeOverridenWithEnvironmentVariable() {
        System.setProperty("NOVA.ID", "oli");
        Nova sut = createSut();
        assertThat(sut.identifier, is("oli"));
    }

    @Test
    void warnOnUnhandledEventCanBeOverridenWithEnvironmentVariable() {
        System.setProperty("NOVA.EVENTS.WARN_ON_UNHANDLED", "true");
        Nova sut = createSut();
        assertThat(sut.eventBus.eventBusConfig.warnOnUnhandledEvents, is(true));
    }

    @Test
    void defaultBackpressureStrategyCanBeOverridenWithEnvironmentVariable() {
        System.setProperty("NOVA.EVENTS.BACKPRESSURE_STRATEGY", BackpressureStrategy.DROP.toString());
        Nova sut = createSut();
        assertThat(sut.eventBus.eventBusConfig.defaultBackpressureStrategy, is(BackpressureStrategy.DROP));
    }

    @Test
    void captureJvmMetricsCanBeOverridenWithEnvironmentVariable() {
        System.setProperty("NOVA.METRICS.CAPTURE_VM_METRICS", "false");
        Nova sut = createSut();
        assertNull(sut.metrics.getMetrics().get("jvm.mem"));
        assertNull(sut.metrics.getMetrics().get("jvm.gc"));
    }


    @Configuration
    @Import(NovaProvidingConfiguration.class)
    public static class MyEmptyConfig {
    }
}

