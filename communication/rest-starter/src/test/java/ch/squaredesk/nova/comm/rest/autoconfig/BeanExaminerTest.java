/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.rest.autoconfig;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.hamcrest.MatcherAssert.assertThat;

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

        assertThat(BeanExaminer.isRestHandler(new MyRestClass1()), Matchers.is(true));
        assertThat(BeanExaminer.isRestHandler(new MyNonRestClass1()), Matchers.is(false));
    }

    @Test
    void needsNonNullBean() throws Exception {
        Throwable t = Assertions.assertThrows(NullPointerException.class,
                () -> BeanExaminer.isRestHandler(null)
        );
        MatcherAssert.assertThat(t.getMessage(), Matchers.containsString("bean"));
    }

}