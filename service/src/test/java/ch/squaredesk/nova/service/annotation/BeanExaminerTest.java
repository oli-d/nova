/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service.annotation;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BeanExaminerTest {
    private BeanExaminer sut = new BeanExaminer();

    @Test
    void properlyDetectsInitHandlers() {
        class MyClassWithHandler {
            @OnServiceInit()
            public void m1() { }
            public void m2() { }
        }
        class MyClassWithoutHandler {
            public void m1(String p1) { }
        }

        assertThat(sut.initHandlersIn(new MyClassWithHandler()).length, is(1));
        assertThat(sut.initHandlersIn(new MyClassWithoutHandler()).length, is(0));
    }

    @Test
    void throwsIfNonPublicMethodIsAnnotatedAsInitHandler() {
        class MyClassWithHandler {
            @OnServiceInit()
            void m1() { }
        }

        assertThrows(IllegalArgumentException.class,
                () -> sut.initHandlersIn(new MyClassWithHandler()));
    }

    @Test
    void throwsIfAnnotatedInitHandlerMethodNeedsParameters() {
        class MyClassWithHandler {
            @OnServiceInit()
            public void m1(String p1) { }
        }

        assertThrows(IllegalArgumentException.class,
                () -> sut.initHandlersIn(new MyClassWithHandler()));
    }
    @Test
    void properlyDetectsStartHandlers() {
        class MyClassWithHandler {
            @OnServiceStartup()
            public void m1() { }
            public void m2() { }
        }
        class MyClassWithoutHandler {
            public void m1(String p1) { }
        }

        assertThat(sut.startupHandlersIn(new MyClassWithHandler()).length, is(1));
        assertThat(sut.startupHandlersIn(new MyClassWithoutHandler()).length, is(0));
    }

    @Test
    void throwsIfNonPublicMethodIsAnnotatedAsStartHandler() {
        class MyClassWithHandler {
            @OnServiceStartup()
            void m1() { }
        }

        assertThrows(IllegalArgumentException.class,
                () -> sut.startupHandlersIn(new MyClassWithHandler()));
    }

    @Test
    void throwsIfAnnotatedStartupHandlerMethodNeedsParameters() {
        class MyClassWithHandler {
            @OnServiceStartup()
            public void m1(String p1) { }
        }

        assertThrows(IllegalArgumentException.class,
                () -> sut.startupHandlersIn(new MyClassWithHandler()));
    }
    @Test
    void properlyDetectsShutdownHandlers() {
        class MyClassWithHandler {
            @OnServiceShutdown()
            public void m1() { }
            public void m2() { }
        }
        class MyClassWithoutHandler {
            public void m1(String p1) { }
        }

        assertThat(sut.shutdownHandlersIn(new MyClassWithHandler()).length, is(1));
        assertThat(sut.shutdownHandlersIn(new MyClassWithoutHandler()).length, is(0));
    }

    @Test
    void throwsIfNonPublicMethodIsAnnotatedAsShutdownHandler() {
        class MyClassWithHandler {
            @OnServiceShutdown()
            void m1() { }
        }

        assertThrows(IllegalArgumentException.class,
                () -> sut.shutdownHandlersIn(new MyClassWithHandler()));
    }

    @Test
    void throwsIfAnnotatedShutdownHandlerMethodNeedsParameters() {
        class MyClassWithHandler {
            @OnServiceShutdown()
            public void m1(String p1) { }
        }

        assertThrows(IllegalArgumentException.class,
                () -> sut.shutdownHandlersIn(new MyClassWithHandler()));
    }

    @Test
    void needsNonNullBean() throws Exception {
        Throwable t = assertThrows(NullPointerException.class,
                () -> sut.shutdownHandlersIn(null)
        );
        assertThat(t.getMessage(), containsString("bean"));
    }

}