/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest;

import org.junit.jupiter.api.Test;

import javax.ws.rs.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

class BeanExaminerTest {
    private BeanExaminer sut = new BeanExaminer();

    @Test
    void properlyDetectsRestEndpointBeansIfTheirClassHasAPathAnnotationAndTheMethodsComeWithAnHttpMethodAndPath() {
        @Path("/")
        class MyRestClass1 {
            @GET
            @Path("m1")
            public void m1(String p1) { }
        }
        @Path("/")
        class MyRestClass2 {
            @Path("m2")
            @POST
            public void m1(String p1) { }
        }
        @Path("/")
        class MyRestClass3 {
            @PUT
            @Path("m3")
            public void m1(String p1) { }
        }
        @Path("/")
        class MyRestClass4 {
            @DELETE
            @Path("m4")
            public void m1(String p1) { }
        }
        @Path("/")
        class MyNonRestClass1 {
            @Path("m5")
            public void m1(String p1) { }
        }
        @Path("/")
        class MyNonRestClass2 {
            @POST
            public void m1(String p1) { }
        }
        class MyNonRestClass3 {
            @POST
            @Path("m6")
            public void m1(String p1) { }
        }

        assertTrue(sut.providesRestEndpoint(new MyRestClass1()));
        assertTrue(sut.providesRestEndpoint(new MyRestClass2()));
        assertTrue(sut.providesRestEndpoint(new MyRestClass3()));
        assertTrue(sut.providesRestEndpoint(new MyRestClass4()));
        assertFalse(sut.providesRestEndpoint(new MyNonRestClass1()));
        assertFalse(sut.providesRestEndpoint(new MyNonRestClass2()));
        assertFalse(sut.providesRestEndpoint(new MyNonRestClass3()));
    }

    @Test
    void needsNonNullBean() throws Exception {
        Throwable t = assertThrows(NullPointerException.class,
                () -> sut.providesRestEndpoint(null)
        );
        assertThat(t.getMessage(), containsString("bean"));
    }

}