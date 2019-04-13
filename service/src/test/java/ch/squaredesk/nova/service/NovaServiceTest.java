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
import ch.squaredesk.nova.service.annotation.OnServiceInit;
import ch.squaredesk.nova.service.annotation.OnServiceShutdown;
import ch.squaredesk.nova.service.annotation.OnServiceStartup;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@Tag("medium")
public class NovaServiceTest {
    @AfterEach
    void tearDown() {
        System.clearProperty(NovaServiceConfiguration.BeanIdentifiers.CONFIG_FILE);
        System.clearProperty(NovaServiceConfiguration.BeanIdentifiers.INSTANCE_IDENTIFIER);
        System.clearProperty(NovaServiceConfiguration.BeanIdentifiers.NAME);
        System.clearProperty(NovaServiceConfiguration.BeanIdentifiers.REGISTER_SHUTDOWN_HOOK);
        System.clearProperty("foo");
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
                UnsatisfiedDependencyException.class,
                () -> MyService.createInstance(MyService.class, MyCrippledConfig.class));
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
    void serviceNameCanBeSpecifiedWithEnvironmentProperty() throws Exception {
        System.setProperty(NovaServiceConfiguration.BeanIdentifiers.NAME, "ABC");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class, NovaServiceConfiguration.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        assertThat(sut.serviceName, is("ABC"));
    }

    @Test
    void defaultServiceNameUsedIfNotSpecified() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class, NovaServiceConfiguration.class);
        ctx.refresh();
        MyService sut = ctx.getBean(MyService.class);

        assertThat(sut.serviceName, is("MyService"));
    }

    @Test
    void defaultConfigsLoadedAutomaticallyIfPresent() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfigWithProperty.class, NovaServiceConfiguration.class);
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
    public static class MyCrippledConfig  {
        @Bean
        public Nova nova() {
            return null;
        }

        @Bean
        public Object serviceInstance() {
            return new MyService();
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
