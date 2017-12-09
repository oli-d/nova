/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest.annotation;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BeanExaminerTest {

    @Test
    void properlyDetectsRestEndpoints() {
        class MyRestClass1 {
            @OnRestRequest("m1")
            public void m1(String p1) { }
            public void m2(String p1) { }
        }
        class MyNonRestClass1 {
            public void m1(String p1) { }
        }

        assertThat(BeanExaminer.restEndpointsIn(new MyRestClass1()).length, is(1));
        assertThat(BeanExaminer.restEndpointsIn(new MyNonRestClass1()).length, is(0));
    }

    @Test
    void throwsIfNonPublicMethodIsAnnotated() {
        class MyRestClass2 {
            @OnRestRequest("m1")
            void m1(String p1) { }
        }

        assertThrows(IllegalArgumentException.class,
                () -> BeanExaminer.restEndpointsIn(new MyRestClass2()));
    }

    @Test
    void needsNonNullBean() throws Exception {
        Throwable t = assertThrows(NullPointerException.class,
                () -> BeanExaminer.restEndpointsIn(null)
        );
        assertThat(t.getMessage(), containsString("bean"));
    }

}