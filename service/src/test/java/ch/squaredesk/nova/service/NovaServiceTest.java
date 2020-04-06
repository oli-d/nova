/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.service;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.metrics.MetricsDump;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Duration;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("medium")
class NovaServiceTest {
    @Test
    void metricsDumpContainsAdditionalInformation() throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();

        MyService sut = new MyService(new ServiceDescriptor("Name", "ID"));

        MetricsDump dump = sut
                .dumpMetricsContinuously(Duration.ofSeconds(1))
                .blockingFirst();

        assertThat(dump.additionalInfo.size(), is(5));
        assertThat(dump.additionalInfo.get(0)._1, is("hostName"));
        assertThat(dump.additionalInfo.get(0)._2, is(inetAddress.getHostName()));
        assertThat(dump.additionalInfo.get(1)._1, is("hostAddress"));
        assertThat(dump.additionalInfo.get(1)._2, is(inetAddress.getHostAddress()));
        assertThat(dump.additionalInfo.get(2)._1, is("serviceName"));
        assertThat(dump.additionalInfo.get(2)._2, is("Name"));
        assertThat(dump.additionalInfo.get(3)._1, is("serviceInstanceId"));
        assertThat(dump.additionalInfo.get(3)._2, is("ID"));
        assertThat(dump.additionalInfo.get(4)._1, is("serviceInstanceName"));
        assertThat(dump.additionalInfo.get(4)._2, is("Name.ID"));
    }

    @Test
    void defaultValuesAreCreatedIfNotPassed() {
        MyService sut = new MyService();

        assertTrue(sut.serviceDescriptor.lifecycleEnabled);
        assertThat(sut.serviceDescriptor.serviceName, is("MyService"));
        assertNotNull(sut.serviceDescriptor.instanceId);
    }

    @Test
    void serviceNameIsCalculatedIfNullWasPassedInDescriptor() {
        MyService sut = new MyService(new ServiceDescriptor(null, "1"));

        assertTrue(sut.serviceDescriptor.lifecycleEnabled);
        assertThat(sut.serviceDescriptor.serviceName, is("MyService"));
        assertThat(sut.serviceDescriptor.instanceId, is("1"));
    }

    @Test
    void startedServiceCannotBeRestartedWithoutShutdown() {
        MyService sut = new MyService();
        sut.start();
        assertTrue(sut.isStarted());

        assertThrows(IllegalStateException.class, sut::start);
    }

    @Test
    void notStartedServiceCanBeShutdownButNothingIsDoneInThatCase() {
        MyService sut = new MyService();

        assertFalse(sut.isStarted());
        sut.shutdown();
        assertFalse(sut.isStarted());
        assertThat(sut.onStartInvocations,is(0));
        assertThat(sut.onShutdownInvocations,is(0));
    }

    @Test
    void exceptionInOnStartPreventsServiceStart() {
        MyBrokenStartService sut = new MyBrokenStartService();
        Throwable t = assertThrows(RuntimeException.class, sut::start);
        assertThat(t.getMessage(), containsString("Error invoking startup handler"));
    }

    @Test
    void exceptionInOnInitPreventsServiceCreation() {
        assertThrows(RuntimeException.class,
                () -> new MyBrokenInitService().start());
    }

    @Test
    void startedServiceCanBeRestartedAfterShutdown() {
        MyService sut = new MyService();

        assertFalse(sut.isStarted());

        sut.start();
        assertTrue(sut.isStarted());
        assertThat(sut.onStartInvocations,is(1));

        sut.shutdown();
        assertFalse(sut.isStarted());
        assertThat(sut.onStartInvocations,is(1));
        assertThat(sut.onShutdownInvocations,is(1));

        sut.start();
        assertTrue(sut.isStarted());
        assertThat(sut.onStartInvocations,is(2));
    }

    @Test
    void lifecycleCallbacksAreBeingInvoked() {
        MyService sut = new MyService();

        assertFalse(sut.isStarted());
        assertThat(sut.onInitInvocations,is(0));
        assertThat(sut.onStartInvocations,is(0));
        assertThat(sut.onShutdownInvocations,is(0));

        sut.start();
        assertTrue(sut.isStarted());
        assertThat(sut.onInitInvocations,is(1));
        assertThat(sut.onStartInvocations,is(1));
        assertThat(sut.onShutdownInvocations,is(0));

        sut.shutdown();
        assertFalse(sut.isStarted());
        assertThat(sut.onStartInvocations,is(1));
        assertThat(sut.onShutdownInvocations,is(1));
        assertThat(sut.onShutdownInvocations,is(1));
    }

    @Test
    void calculatedServiceNameUsedForMetricDumpsIfNotSpecified() {
        MyService sut = new MyService();
        sut.start();

        MetricsDump metricsDump = sut.dumpMetricsContinuously(Duration.ofMillis(5)).first(new MetricsDump(new HashMap<>())).blockingGet();

        assertThat(metricsDump.additionalInfo.stream().anyMatch(p -> "serviceName".equals(p._1) && "MyService".equals(p._2)), is(true));
    }

    public static class MyBrokenInitService extends MyService {
        @Override
        public void onInit() {
            throw new RuntimeException("for test");
        }
    }

    public static class MyBrokenStartService extends MyService {
        @Override
        public void onStart() {
            throw new RuntimeException("for test");
        }
    }

    public static class MyService extends NovaService {
        private int onInitInvocations = 0;
        private int onStartInvocations = 0;
        private int onShutdownInvocations = 0;

        protected MyService() {
            this(null);
        }

        protected MyService(ServiceDescriptor serviceDescriptor) {
            super(Nova.builder().build(), serviceDescriptor);
            registerInitHandler(this::onInit);
            registerStartupHandler(this::onStart);
            registerShutdownHandler(this::onShutdown);
        }

        public void onInit() {
            onInitInvocations++;
        }

        public void onStart() {
            onStartInvocations++;
        }

        public void onShutdown() {
            onShutdownInvocations++;
        }
    }
}
