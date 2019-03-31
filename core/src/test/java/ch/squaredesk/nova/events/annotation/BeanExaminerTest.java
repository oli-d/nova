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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BeanExaminerTest {
    private BeanExaminer sut = new BeanExaminer();

    @Test
    void examineNeedsNonNullBean() throws Exception {
        Throwable t = assertThrows(NullPointerException.class,
                () -> sut.examine(null));
        assertThat(t.getMessage(), containsString("bean"));
    }

    @Test
    void allAnnotatedMethodsFound() {
        class MyClass {
            @OnEvent("e1")
            public void m1() {}
            @OnEvent("e2")
            public void m2() {}
            @OnEvent("e3")
            public void m3() {}
        }

        EventHandlerDescription[] detectedHandlers = sut.examine(new MyClass());
        Arrays.sort(detectedHandlers, Comparator.comparing(handler -> handler.methodToInvoke.getName()));
        assertThat(detectedHandlers.length, is(3));
        assertThat(detectedHandlers[0].methodToInvoke.getName(), is("m1"));
        assertThat(detectedHandlers[1].methodToInvoke.getName(), is("m2"));
        assertThat(detectedHandlers[2].methodToInvoke.getName(), is("m3"));
    }

    @Test
    void handlersCanSupportMultipleEvents() {
        class MyClass {
            @OnEvent({"e1","e2","e3"})
            public void m1() {}
            public void m2() {}
            public void m3() {}
        }

        EventHandlerDescription[] detectedHandlers = sut.examine(new MyClass());
        assertThat(detectedHandlers.length, is(1));
        assertThat(Arrays.asList(detectedHandlers[0].events), containsInAnyOrder("e1","e2","e3"));
    }
}