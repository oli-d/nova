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
import ch.squaredesk.nova.metrics.MetricsDump;
import ch.squaredesk.nova.service.annotation.OnServiceInit;
import ch.squaredesk.nova.service.annotation.OnServiceShutdown;
import ch.squaredesk.nova.service.annotation.OnServiceStartup;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import ch.squaredesk.nova.tuples.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("medium")
class NovaServiceTest {
    @AfterEach
    void tearDown() {
        System.clearProperty(NovaServiceConfiguration.BeanIdentifiers.CONFIG_FILE);
        System.clearProperty(NovaServiceConfiguration.BeanIdentifiers.INSTANCE_IDENTIFIER);
        System.clearProperty(NovaServiceConfiguration.BeanIdentifiers.NAME);
        System.clearProperty(NovaServiceConfiguration.BeanIdentifiers.REGISTER_SHUTDOWN_HOOK);
        System.clearProperty("foo");
    }

    @Test
    void metricsDumpContainsServiceInformation() throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        MyService sut = MyService.createInstance(MyService.class, MyConfigWithInstanceIdAndServiceName.class);

        MetricsDump dump = sut
                .dumpMetricsContinuously(1, TimeUnit.MILLISECONDS)
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
    void startedServiceCannotBeRestartedWithoutShutdown() {
        MyService sut = MyService.createInstance(MyService.class, MyConfig.class);
        sut.start();
        assertTrue(sut.isStarted());

        assertThrows(IllegalStateException.class, sut::start);
    }

    @Test
    void notStartedServiceCanBeShutdownButNothingIsDoneInThatCase() {
        MyService sut = MyService.createInstance(MyService.class, MyConfig.class);

        assertFalse(sut.isStarted());
        sut.shutdown();
        assertFalse(sut.isStarted());
        assertThat(sut.onStartInvocations,is(0));
        assertThat(sut.onShutdownInvocations,is(0));
    }

    @Test
    void exceptionInOnStartPreventsServiceStart() {
        MyBrokenStartService sut = MyBrokenStartService
                .createInstance(MyBrokenStartService.class, MyConfigForBrokenStartService.class);
        Throwable t = assertThrows(RuntimeException.class, sut::start);
        assertThat(t.getMessage(), containsString("Error invoking startup handler"));
    }

    @Test
    void exceptionInOnInitPreventsServiceCreation() {
        assertThrows(BeanInitializationException.class,
                () -> MyBrokenInitService.createInstance(MyBrokenInitService.class, MyConfigForBrokenInitService.class));
    }

    @Test
    void earlyExceptionForServiceConfigurationThatIsNotProperlyAnnotated() {
        Throwable t = assertThrows(NoSuchBeanDefinitionException.class,
                () -> MyService.createInstance(MyService.class, MyConfigWithoutBeanAnnotation.class));
        assertThat(t.getMessage(), containsString("No qualifying bean of type"));
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
        MyService sut = NovaService.createInstance(MyService.class, MyConfig.class);

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
    void serviceNameCanBeSpecifiedWithEnvironmentProperty() {
        System.setProperty(NovaServiceConfiguration.BeanIdentifiers.NAME, "ABC");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class, NovaServiceConfiguration.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        assertThat(sut.serviceName, is("ABC"));
    }

    @Test
    void calculatedServiceNameUsedForMetricDumpsIfNotSpecified() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class, NovaServiceConfiguration.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        List<Pair<String, String>> additionalInfoForMetricsDump = sut.calculateAdditionalInfoForMetricsDump();

        assertThat(additionalInfoForMetricsDump.stream().anyMatch(p -> "hostName".equals(p._1)), is(true));
        assertThat(additionalInfoForMetricsDump.stream().anyMatch(p -> "hostAddress".equals(p._1)), is(true));
        assertThat(additionalInfoForMetricsDump.stream().anyMatch(p -> "serviceName".equals(p._1) && "MyService".equals(p._2)), is(true));
        assertThat(additionalInfoForMetricsDump.stream().anyMatch(p -> "serviceInstanceId".equals(p._1) && p._2 != null), is(true));
        assertThat(additionalInfoForMetricsDump.stream().anyMatch(p -> "serviceInstanceName".equals(p._1) && p._2 != null), is(true));
    }

    @Test
    void defaultConfigsLoadedAutomaticallyIfPresent() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(NovaServiceConfiguration.class, MyConfigWithProperty.class);
        ctx.refresh();
        assertThat(ctx.getBean("foo"), is ("bar"));
    }

    @Test
    void noProblemIfSpecificConfigFileDoesNotExist() {
        System.setProperty(NovaServiceConfiguration.BeanIdentifiers.CONFIG_FILE, "doesn'tExist");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfigWithProperty.class, NovaServiceConfiguration.class);
        ctx.refresh();
        assertThat(ctx.getBean("foo"), is ("bar"));
    }

    @Test
    void specificConfigFileIsLoadedIfPresentAndOverridesDefaultConfig() {
        System.setProperty(NovaServiceConfiguration.BeanIdentifiers.CONFIG_FILE, "override.properties");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfigWithProperty.class, NovaServiceConfiguration.class);
        ctx.refresh();
        assertThat(ctx.getBean("foo"), is ("baz"));
    }

    @Test
    void specificConfigItemsCanAlsoBeSetViaEnvironmentVariable() {
        System.setProperty("foo", "baz");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfigWithProperty.class, NovaServiceConfiguration.class);
        ctx.refresh();
        assertThat(ctx.getBean("foo"), is ("baz"));
    }


    @Component
    public static class MyBrokenInitService extends NovaService {
        @OnServiceInit
        public void onInit() {
            throw new RuntimeException("for test");
        }
    }

    @Component
    public static class MyBrokenStartService extends NovaService {
        @OnServiceStartup
        public void onStart() {
            throw new RuntimeException("for test");
        }
    }

    @Component
    public static class MyService extends NovaService {
        private int onInitInvocations = 0;
        private int onStartInvocations = 0;
        private int onShutdownInvocations = 0;

        @OnServiceInit
        public void onInit() {
            onInitInvocations++;
        }

        @OnServiceStartup
        public void onStart() {
            onStartInvocations++;
        }

        @OnServiceShutdown
        public void onShutdown() {
            onShutdownInvocations++;
        }
    }


    @Configuration
    public static class MyConfig {
        @Autowired
        Environment env;

        @Bean(NovaServiceConfiguration.BeanIdentifiers.INSTANCE)
        public MyService serviceInstance() {
            return new MyService();
        }
    }

    @Configuration
    public static class MyConfigWithInstanceIdAndServiceName {
        @Autowired
        Environment env;

        @Bean(NovaServiceConfiguration.BeanIdentifiers.INSTANCE)
        public MyService serviceInstance() {
            return new MyService();
        }

        @Bean(NovaServiceConfiguration.BeanIdentifiers.INSTANCE_IDENTIFIER)
        public String instanceId() {
            return "ID";
        }

        @Bean(NovaServiceConfiguration.BeanIdentifiers.NAME)
        public String serviceName() {
            return "Name";
        }
    }

    @Configuration
    public static class MyConfigWithProperty {
        @Autowired
        Environment env;

        @Bean(NovaServiceConfiguration.BeanIdentifiers.INSTANCE)
        public MyService serviceInstance() {
            return new MyService();
        }

        @Bean
        public String foo() {
            return env.getProperty("foo");
        }
    }

    @Configuration
    public static class MyConfigForBrokenStartService {
        @Bean(NovaServiceConfiguration.BeanIdentifiers.INSTANCE)
        public MyBrokenStartService serviceInstance() {
            return new MyBrokenStartService();
        }
    }

    @Configuration
    public static class MyConfigForBrokenInitService {
        @Bean(NovaServiceConfiguration.BeanIdentifiers.INSTANCE)
        public MyBrokenInitService serviceInstance() {
            return new MyBrokenInitService();
        }
    }

    @Configuration
    public static class MyConfigWithoutBeanAnnotation {
        public MyService serviceInstance() {
            return new MyService();
        }
    }
}
