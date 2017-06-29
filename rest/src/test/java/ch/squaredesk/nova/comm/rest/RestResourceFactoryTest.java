package ch.squaredesk.nova.comm.rest;

import org.glassfish.jersey.server.model.Resource;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
        assertThat(resource.getPath(), is(resourceDescriptor.path));
        assertThat(resource.getHandlerInstances().size(), is(1));
        assertThat(resource.getHandlerInstances(), contains(myBean));
    }
}