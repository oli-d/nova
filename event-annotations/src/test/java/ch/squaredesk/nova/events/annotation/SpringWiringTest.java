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
import ch.squaredesk.nova.events.EventBus;
import io.reactivex.BackpressureStrategy;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SpringWiringTest {

    private AnnotationConfigApplicationContext getApplicationContext() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        return ctx;
    }

    @Test
    public void eventHandlersProperlyRegistered() throws Exception {
        AnnotationConfigApplicationContext ctx = getApplicationContext();
        MyAnnotatedBean sut = ctx.getBean(MyAnnotatedBean.class);
        EventBus eventBus = ctx.getBean(Nova.class).eventBus;

        eventBus.emit("e1");
        assertThat(sut.invokedEvent1, is(2));

        eventBus.emit("e2");
        assertThat(sut.invokedEvent1, is(2));
        assertThat(sut.invokedEvent2, is(1));
    }

    @Test
    public void eventHandlersProperlyReceiveEventContextIfSignatureOk() throws Exception {
        AnnotationConfigApplicationContext ctx = getApplicationContext();
        MyAnnotatedBean sut = ctx.getBean(MyAnnotatedBean.class);
        EventBus eventEmitter = ctx.getBean(Nova.class).eventBus;

        eventEmitter.emit("e3", "string", 1.0, "anotherString");
        assertThat(sut.invokedEvent1, is(0));
        assertThat(sut.invokedEvent2, is(0));
        assertThat(sut.invokedEvent3_1, is(1));
        assertThat(sut.event3_1P1, is("string"));
        assertNotNull(sut.event3_1Context);
        assertThat(sut.event3_1Context.eventBus, Matchers.sameInstance(ctx.getBean(Nova.class).eventBus));
        assertThat(sut.event3_1Context.metrics, Matchers.sameInstance(ctx.getBean(Nova.class).metrics));
        assertThat(sut.invokedEvent3_2, is(1));
        assertThat(sut.event3_2P1, is("string"));
        assertThat(sut.event3_2P2, is(1.0));
        assertThat(sut.event3_2P3, is("anotherString"));
    }

    @Test
    public void eventHandlerRegistrationConsidersBusinessThreadFlag() throws Exception {
        AnnotationConfigApplicationContext ctx = getApplicationContext();
        MyBean1 myBean1 = ctx.getBean(MyBean1.class);
        EventBus eventBus = ctx.getBean(Nova.class).eventBus;

        Thread[] emitterThreads = new Thread[5];
        for (int i = 0; i < emitterThreads.length; i++) {
            emitterThreads[i] = new Thread(() -> eventBus.emit("e"));
        }
        for (int i = 0; i < emitterThreads.length; i++) {
            emitterThreads[i].start();
        }
        for (int i = 0; i < emitterThreads.length; i++) {
            emitterThreads[i].join();
        }

        Set<Thread> distinctThreads1 = new HashSet<>();
        distinctThreads1.addAll(myBean1.listInvocationThreads);
        assertThat(distinctThreads1.size(),is(1));
    }


    @Import(AnnotationEnablingConfiguration.class)
    public static class MyConfig extends ch.squaredesk.nova.events.annotation.NovaProvidingConfiguration {
        @Bean()
        public MyAnnotatedBean getMyBean() {
            return new MyAnnotatedBean();
        }
        @Bean()
        public MyBean1 getMyBean1() {
            return new MyBean1();
        }
        @Bean()
        public MyBean2 getMyBean2() {
            return new MyBean2();
        }
    }

    public static class MyBean1 {
        private List<String> listInvocationParams = new ArrayList<>();
        private List<Thread> listInvocationThreads = new ArrayList<>();

        @OnEvent(value = "e", dispatchOnBusinessLogicThread = true, backpressureStrategy = BackpressureStrategy.BUFFER)
        public void onE1(String f) throws Exception {
            TimeUnit.MILLISECONDS.sleep(10);
            listInvocationParams.add(f);
            listInvocationThreads.add(Thread.currentThread());
        }
    }

    public static class MyBean2 {
        private List<String> listInvocationParams = new ArrayList<>();
        private List<Thread> listInvocationThreads = new ArrayList<>();
        @OnEvent(value = "e", enableInvocationTimeMetrics = false, backpressureStrategy = BackpressureStrategy.LATEST)
        public void onE1(String f) throws Exception {
            TimeUnit.MILLISECONDS.sleep(10);
            listInvocationParams.add(f);
            listInvocationThreads.add(Thread.currentThread());
        }
    }

    public static class MyAnnotatedBean {
        private int invokedEvent1 = 0;
        private int invokedEvent2 = 0;
        private int invokedEvent3_1 = 0;
        private String event3_1P1 = null;
        private EventContext event3_1Context = null;
        private int invokedEvent3_2 = 0;
        private String event3_2P1 = null;
        private Double event3_2P2 = null;
        private String event3_2P3 = null;

        public MyAnnotatedBean() {
        }

        @OnEvent("e1")
        public void handleEventOneWithString(String s) {
            invokedEvent1 ++;
        }
        @OnEvent("e1")
        public void handleEventOneWithString(String s, Double d) {
            invokedEvent1 ++;
        }
        @OnEvent("e2")
        public void handleEventTwo(String s, Double d) {
            invokedEvent2 ++;
        }
        @OnEvent("e3")
        public void handleEventThreeOne(String s, EventContext eventContext) {
            invokedEvent3_1 ++;
            event3_1P1 = s;
            event3_1Context = eventContext;
        }
        @OnEvent("e3")
        public void handleEventThreeTwo(String s, Double d, String s2) {
            invokedEvent3_2 ++;
            event3_2P1 = s;
            event3_2P2 = d;
            event3_2P3 = s2;
        }
    }

}