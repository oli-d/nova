/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service;

import ch.squaredesk.nova.Nova;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class NovaServiceTest {
    @AfterEach
    void tearDown() {
        System.clearProperty("NOVA.SERVICE.CAPTURE_JVM_METRICS");
        System.clearProperty("NOVA.SERVICE.NAME");
        System.clearProperty("NOVA.SERVICE.INSTANCE_ID");
    }

    @Test
    void serviceCannotBeStartedWithoutConfig() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.refresh();

        assertThrows(
                NoSuchBeanDefinitionException.class,
                () -> ctx.getBean(MyService.class));
    }

    @Test
    void serviceCannotBeCreatedWithConfigThatDoesntReturnNovaInstance() {
        assertThrows(
                BeanCreationException.class,
                () -> MyService.createInstance(MyService.class, MyCrippledConfig.class));
    }

    @Test
    void serviceCannotBeCreatedWithConfigThatIsntAnnotatedWithConfiguration() {
        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> MyService.createInstance(MyService.class, MyConfigWithoutConfigurationAnnotation.class));
        assertThat(t.getMessage(), containsString("must be annotated with @Configuration"));
    }

    @Test
    void startedServiceCannotBeRestartedWithoutShutdown() {
        MyService sut = MyService.createInstance(MyService.class, MyConfig.class);
        sut.start();
        assertTrue(sut.isStarted());

        assertThrows(IllegalStateException.class, sut::start);
    }

    @Test
    void notStartedServiceCanBeShutdown() {
        MyService sut = MyService.createInstance(MyService.class, MyConfig.class);

        assertFalse(sut.isStarted());
        sut.shutdown();
        assertFalse(sut.isStarted());
        assertThat(sut.onStartInvocations,is(0));
        assertThat(sut.onShutdownInvocations,is(1));
    }

    @Test
    void exceptionInOnStartPreventsServiceStart() {
        MyBrokenStartService sut = MyBrokenStartService
                .createInstance(MyBrokenStartService.class, MyConfigForBrokenStartService.class);
        Throwable t = assertThrows(RuntimeException.class, sut::start);
        assertThat(t.getMessage(), containsString("unable to start"));
    }

    @Test
    void exceptionInOnInitPreventsServiceCreation() {
        assertThrows(BeanCreationException.class,
                () -> MyBrokenInitService.createInstance(MyBrokenInitService.class, MyConfigForBrokenInitService.class));
    }

    @Test
    void earlyExceptionForServiceConfigurationThatIsNotProperlyAnnotated() {
        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> MyService.createInstance(MyService.class, MyConfigWithoutBeanAnnotation.class));
        assertThat(t.getMessage(), containsString("must be annotated with @Bean"));
    }

    @Test
    void startedServiceCanBeRestartedAfterShutdown() {
        MyService sut = MyService.createInstance(MyService.class, MyConfig.class);

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
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext();

        ctx.register(MyConfig.class);
        ctx.refresh();

        MyService sut = ctx.getBean(MyService.class);

        assertFalse(sut.isStarted());
        assertThat(sut.onInitInvocations,is(1));
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
    void serviceNameCanBeSpecifiedWithEnvironmentProperty() throws Exception {
        System.setProperty("NOVA.SERVICE.NAME", "ABC");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        assertThat(sut.serviceName, is("ABC"));
    }

    @Test
    void serviceNameDerivedFromConfigClassNameIfNotSpecified() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        assertThat(sut.serviceName, is(
                MyConfig.class.getName()
                        .replace(getClass().getPackage().getName() + ".", "")
                        .replace("Config","")));
    }

    @Test
    void instanceIdCanBeSpecifiedWithEnvironmentProperty() throws Exception {
        System.setProperty("NOVA.SERVICE.INSTANCE_ID", "XYZ");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        assertThat(sut.instanceId, is("XYZ"));
    }

    @Test
    void jvmMetricsAreCapturedByDefault() throws Exception {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        assertTrue(sut.captureJvmMetrics);

        TestObserver<ServiceMetricsSet> observer = sut.serviceMetrics(100, TimeUnit.MILLISECONDS).test();
        observer.await(1, TimeUnit.SECONDS);
        assertThat(observer.valueCount(), greaterThan(0));
        ServiceMetricsSet sms = observer.values().get(0);
        assertNotNull(sms.gauges.get("jvm.gc.PS-MarkSweep.count"));
    }

    @Test
    void jvmMetricsCapturingCanBeSwitchedOffWithEnvironmentProperty() throws Exception {
        System.setProperty("NOVA.SERVICE.CAPTURE_JVM_METRICS", "false");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        assertFalse(sut.captureJvmMetrics);

        TestObserver<ServiceMetricsSet> observer = sut.serviceMetrics(100, TimeUnit.MILLISECONDS).test();
        observer.await(1, TimeUnit.SECONDS);
        assertThat(observer.valueCount(), greaterThan(0));
        ServiceMetricsSet sms = observer.values().get(0);
        assertNull(sms.gauges.get("jvm.gc.PS-MarkSweep.count"));
    }

    @Test
    void serviceMetricsCallNeedsIntervalLargerThanZero() {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext();

        ctx.register(MyConfig.class);
        ctx.refresh();

        MyService sut = ctx.getBean(MyService.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sut.serviceMetrics(-1, TimeUnit.SECONDS));
        assertThat(ex.getMessage(),containsString("interval must be greater than 0"));
        ex = assertThrows(IllegalArgumentException.class,
                () -> sut.serviceMetrics(0, TimeUnit.SECONDS));
        assertThat(ex.getMessage(),containsString("interval must be greater than 0"));
    }

    @Test
    void serviceMetricsCallNeedsNonNullTimeUnit() {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext();

        ctx.register(MyConfig.class);
        ctx.refresh();

        MyService sut = ctx.getBean(MyService.class);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> sut.serviceMetrics(5, null));
        assertThat(ex.getMessage(),containsString("timeUnit must not be null"));
    }

    @Test
    void serviceMetricsIncludeServerDetails() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        TestObserver<ServiceMetricsSet> observer = sut.serviceMetrics(100, TimeUnit.MILLISECONDS).test();
        observer.await(1, TimeUnit.SECONDS);
        assertThat(observer.valueCount(), greaterThan(0));
        ServiceMetricsSet sms = observer.values().get(0);
        InetAddress inetAddress = InetAddress.getLocalHost();
        assertThat(sms.serverName, is(inetAddress.getHostName()));
        assertThat(sms.serverAddress, is(inetAddress.getHostAddress()));
    }

    @Component
    public static class MyBrokenInitService extends NovaService {
        @Override
        protected void onInit() {
            throw new RuntimeException("for test");
        }
    }

    @Component
    public static class MyBrokenStartService extends NovaService {
        @Override
        protected void onStart() {
            throw new RuntimeException("for test");
        }
    }

    @Component
    public static class MyService extends NovaService {
        private int onInitInvocations = 0;
        private int onStartInvocations = 0;
        private int onShutdownInvocations = 0;

        @Override
        protected void onInit() {
            onInitInvocations++;
        }

        @Override
        protected void onStart() {
            onStartInvocations++;
        }

        @Override
        protected void onShutdown() {
            onShutdownInvocations++;
        }
    }


    @Configuration
    public static class MyCrippledConfig extends NovaServiceConfiguration {
        @Override
        @Bean
        public Nova getNova() {
            return null;
        }

        @Bean
        public Object createServiceInstance() {
            return new MyService();
        }
    }

    @Configuration
    public static class MyConfig extends NovaServiceConfiguration<MyService> {
        @Bean
        public MyService createServiceInstance() {
            return new MyService();
        }
    }

    @Configuration
    public static class MyConfigForBrokenStartService extends NovaServiceConfiguration<MyBrokenStartService> {
        @Bean
        public MyBrokenStartService createServiceInstance() {
            return new MyBrokenStartService();
        }
    }

    @Configuration
    public static class MyConfigForBrokenInitService extends NovaServiceConfiguration<MyBrokenInitService> {
        @Bean
        public MyBrokenInitService createServiceInstance() {
            return new MyBrokenInitService();
        }
    }

    @Configuration
    public static class MyConfigWithoutBeanAnnotation extends NovaServiceConfiguration<MyService> {
        public MyService createServiceInstance() {
            return new MyService();
        }
    }

    public static class MyConfigWithoutConfigurationAnnotation extends NovaServiceConfiguration<MyService> {
        @Bean
        public MyService createServiceInstance() {
            return new MyService();
        }
    }
}
