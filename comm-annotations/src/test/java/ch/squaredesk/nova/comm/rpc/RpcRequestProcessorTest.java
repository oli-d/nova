/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.metrics.Metrics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;

class RpcRequestProcessorTest {
    private RpcRequestProcessor<Object, MyRpcInvocation> sut = new RpcRequestProcessor<>(new Metrics());

    @Test
    void customDefaultHandlerMustNotBeNull() {
        NullPointerException npe = Assertions.assertThrows(
                NullPointerException.class,
                () -> sut.onUnregisteredRequest(null));
        assertThat(npe.getMessage(), containsString("must not be null"));
    }

    @Test
    void customUncaughtExceptionHandlerMustNotBeNull() {
        NullPointerException npe = Assertions.assertThrows(
                NullPointerException.class,
                () -> sut.onProcessingException(null));
        assertThat(npe.getMessage(), containsString("must not be null"));
    }

    @Test
    void invokingWithUnregisteredRequestTypeInvokesDefaultHandlerIfProvided() throws Exception {
        RpcInvocation[] rpcInvocationHolder = new RpcInvocation[1];
        sut.onUnregisteredRequest(rpci -> rpcInvocationHolder[0]=rpci);
        MyRpcInvocation invocation = new MyRpcInvocation("");

        sut.accept(invocation);

        assertThat(rpcInvocationHolder[0], sameInstance(invocation));
    }

    @Test
    void invokingWithNoRequestInvokesDefaultHandlerIfProvided() throws Exception {
        RpcInvocation[] rpcInvocationHolder = new RpcInvocation[1];
        sut.onUnregisteredRequest(rpci -> rpcInvocationHolder[0]=rpci);
        MyRpcInvocation invocation = new MyRpcInvocation(null);

        sut.accept(invocation);

        assertThat(rpcInvocationHolder[0], sameInstance(invocation));
    }

    @Test
    void registeringTheSameRequestTypeAgainThrows() {
        sut.register(String.class, s -> {});
        IllegalArgumentException iae = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.register(String.class, s -> {}));
        assertThat(iae.getMessage(), containsString("already registered"));
    }

    @Test
    void registeredFunctionsGetProperlyInvoked() {
        class MyObject {
            private String value;

            private MyObject(String value) { this.value = value;}
        }
        class MyExtendedObject extends MyObject {
            private String valueToo;

            private MyExtendedObject(String value, String valueToo) { super(value); this.valueToo = value;}
        }
        Object[] resultHolder = new Object[1];
        sut.register(String.class, x -> {
            resultHolder[0] = "String";
        });
        sut.register(Integer.class, x -> {
            resultHolder[0] = "Integer";
        });
        sut.register(Double.class, x -> {
            resultHolder[0] = "Double";
        });
        sut.register(MyExtendedObject.class, x -> {
            resultHolder[0] = "Extended";
        });
        sut.register(MyObject.class, x -> {
            resultHolder[0] = "MyObject";
        });

        sut.accept(new MyRpcInvocation("value"));
        assertThat(resultHolder[0], is("String"));
        sut.accept(new MyRpcInvocation(Integer.valueOf(1)));
        assertThat(resultHolder[0], is("Integer"));
        sut.accept(new MyRpcInvocation(2));
        assertThat(resultHolder[0], is("Integer"));
        sut.accept(new MyRpcInvocation(Double.valueOf(3.0)));
        assertThat(resultHolder[0], is("Double"));
        sut.accept(new MyRpcInvocation(3.0));
        assertThat(resultHolder[0], is("Double"));
        sut.accept(new MyRpcInvocation(new MyObject("val")));
        assertThat(resultHolder[0], is("MyObject"));
        sut.accept(new MyRpcInvocation(new MyExtendedObject("val", "val2")));
        assertThat(resultHolder[0], is("Extended"));
    }

    private class MyRpcInvocation extends RpcInvocation<Object, Void, String, Void> {
        private MyRpcInvocation(Object request) {
            super(request, null, null, null);
        }
    }
}