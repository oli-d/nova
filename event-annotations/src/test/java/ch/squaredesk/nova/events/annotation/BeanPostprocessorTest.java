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

public class BeanPostprocessorTest {
    /*
    private EventBus eventBus;
    private Metrics metrics;
    private EventHandlingBeanPostprocessor sut;

    @BeforeEach
    void setup() {
        Nova nova = Nova.builder().build();
        eventBus = nova.eventBus;
        metrics = nova.metrics;
        sut = new EventHandlingBeanPostprocessor("BPTTest", eventBus, metrics);
    }

    @Test
    void eventHandlersProperlyRegistered() throws Exception {
        class MyBean {
            private int numInvoked = 0;
            @OnEvent("e1")
            public void onE1(String f) {
                numInvoked++;
            }
            @OnEvent({"e1","e2"})
            public void onE1andE2(String f) {
                numInvoked++;
            }
        }
        MyBean myBean = new MyBean();

        sut.postProcessAfterInitialization(myBean,"daBean");

        eventBus.emit("myEvent");
        assertThat(myBean.numInvoked, is(0));
        eventBus.emit("e1");
        assertThat(myBean.numInvoked, is(2));
        eventBus.emit("e2");
        assertThat(myBean.numInvoked, is(3));
    }

    @Test
    void eventHandlerRegistrationConsidersTimeMeasurementFlag() throws Exception {
        class MyBean1 {
            @OnEvent(value = "e1")
            public void onE1(String f) {
            }
        }
        class MyBean2 {
            @OnEvent(value = "e1", enableInvocationTimeMetrics = false)
            public void onE1(String f) {
            }
        }
        MyBean1 myBean1 = new MyBean1();
        MyBean2 myBean2 = new MyBean2();

        sut.postProcessAfterInitialization(myBean1,"b1");
        sut.postProcessAfterInitialization(myBean2,"b2");

        long numMeasurements = metrics.getMetrics().keySet().stream()
                .filter(key -> key.contains("MyBean1") && key.contains("e1") && key.contains("invocationTime"))
                .count();
        assertThat(numMeasurements, is(1L));
    }
*/
}