/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service.admin;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanExaminerTest {
    private BeanExaminer sut = new BeanExaminer();

    @Test
    void consumerCalledForAllAnnotatedMethods() {
        class MyClass {
            @OnAdminCommand("e1")
            public String m1(String p1) { return "";}
            @OnAdminCommand({"e2", "e3"})
            public String m2(String p1, String p2) { return "";}
        }

        Map<String,String[]> detectedHandlers = new HashMap<>();
        sut.examine(new MyClass(), cfg -> detectedHandlers.put(cfg.methodToInvoke.getName(), cfg.parameterNames));

        assertThat(detectedHandlers.keySet(), containsInAnyOrder("m1","m2"));
        assertThat(detectedHandlers.get("m1"), is(new String[]{"e1"}));
        assertThat(detectedHandlers.get("m2"), is(new String[]{"e2", "e3"}));
    }

    @Test
    void examineNeedsNonNullBean() throws Exception {
        Throwable t = assertThrows(NullPointerException.class,
                () -> sut.examine(null, cfg -> {})
        );
        assertThat(t.getMessage(), containsString("bean"));
    }

    @Test
    void examineNeedsNonNullConfigConsumer() throws Exception {
        Throwable t = assertThrows(NullPointerException.class,
                () -> sut.examine(new Object(), null)
        );
        assertThat(t.getMessage(), containsString("configConsumer"));
    }

    @Test
    void examineThrowsIfMethodDetectedThatDoesntReturnString() {
        class MyClass {
            @OnAdminCommand({"e2", "e3"})
            public void m2() {}
        }

        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> sut.examine(new MyClass(), config -> {})
        );
        assertThat(t.getMessage(), startsWith("Method m2 must return String"));
    }

    @Test
    void examineThrowsIfTooManyParametersDefined() {
        class MyClass {
            @OnAdminCommand("e1")
            public String m1() { return ""; }
            @OnAdminCommand("e1")
            public String m2(String p1) { return ""; }
        }

        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> sut.examine(new MyClass(), config -> {})
        );
        assertThat(t.getMessage(), is("Invalid parameter definition. 1 parameter(s) defined, 0 expected."));
    }

    @Test
    void examineThrowsIfNotEnoughParametersDefined() {
        class MyClass {
            @OnAdminCommand("e1")
            public String m2(String p1, String p2) { return ""; }
        }

        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> sut.examine(new MyClass(), config -> {})
        );
        assertThat(t.getMessage(), is("Invalid parameter definition. 1 parameter(s) defined, 2 expected."));
    }

}