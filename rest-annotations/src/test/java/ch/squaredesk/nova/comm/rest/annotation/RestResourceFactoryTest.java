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

import org.glassfish.jersey.server.model.Resource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestResourceFactoryTest {
    private RestResourceDescriptor resourceDescriptor = RestResourceDescriptor.from("fooPath");

    @Test
    void exceptionForNullResourceDescriptor() throws Exception {
        class MyBean {
            public String foo() {
                return "foo";
            }
        }
        MyBean myBean = new MyBean();
        Method method = MyBean.class.getMethod("foo");
        assertThrows(NullPointerException.class,
                () -> RestResourceFactory.resourceFor(null, myBean, method));
    }

    @Test
    void exceptionForNullHandlerBean() throws Exception {
        class MyBean {
            public String foo() {
                return "foo";
            }
        }
        Method method = MyBean.class.getMethod("foo");
        assertThrows(NullPointerException.class,
                () -> RestResourceFactory.resourceFor(resourceDescriptor, null, method));
    }

    @Test
    void exceptionForNullHandlerMethod() throws Exception {
        class MyBean {
            public String foo() {
                return "foo";
            }
        }
        MyBean myBean = new MyBean();
        assertThrows(NullPointerException.class,
                () -> RestResourceFactory.resourceFor(resourceDescriptor, myBean, null));
    }

    @Test
    void testFactoryMethod() throws Exception {
        class MyBean {
            public String foo() {
                return "foo";
            }
        }
        MyBean myBean = new MyBean();
        Method method = MyBean.class.getMethod("foo");
        Resource resource = RestResourceFactory.resourceFor(resourceDescriptor, myBean, method);
        assertNotNull(resource);
        assertThat(resource.getPath(), Matchers.is(resourceDescriptor.path));
        assertThat(resource.getHandlerInstances().size(), is(1));
        assertThat(resource.getHandlerInstances(), contains(myBean));
    }
}