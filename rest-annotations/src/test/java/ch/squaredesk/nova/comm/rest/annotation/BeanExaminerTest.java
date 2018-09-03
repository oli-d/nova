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

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BeanExaminerTest {

    @Test
    void properlyDetectsRestEndpoints() {
        @Path("m1")
        class MyRestClass1 {
            @GET
            public void m1(String p1) { }
            public void m2(String p1) { }
        }
        class MyNonRestClass1 {
            @Path("m1")
            public void m1(String p1) { }
        }

        assertThat(BeanExaminer.isRestHandler(new MyRestClass1()), is(true));
        assertThat(BeanExaminer.isRestHandler(new MyNonRestClass1()), is(false));
    }

    @Test
    void needsNonNullBean() throws Exception {
        Throwable t = assertThrows(NullPointerException.class,
                () -> BeanExaminer.isRestHandler(null)
        );
        assertThat(t.getMessage(), containsString("bean"));
    }

}