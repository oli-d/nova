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

import ch.squaredesk.nova.tuples.Pair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.junit.MatcherAssert.assertThat;

class RpcRequestProcessorTest {
    private RpcRequestProcessor<MyRpcInvocation, Object, String> sut = new RpcRequestProcessor<>();

    @Test
    void exceptionInHandlerFunctionInvokesErrorFunctionIfProvided() throws Exception {
        RuntimeException exception = new RuntimeException("for test");
        sut.onProcessingException((rpci,error) -> error.getMessage());
        sut.register(String.class, x -> { throw exception; });

        Pair<MyRpcInvocation, String> pair = sut.apply(new MyRpcInvocation(""));

        assertThat(pair._2, is(exception.getMessage()));
    }

    @Test
    void invokingWithUnregisteredRequestTypeInvokesErrorFunctionIfProvided() throws Exception {
        sut.onMissingRequestProcessor(rpci -> "oops");

        Pair<MyRpcInvocation, String> pair = sut.apply(new MyRpcInvocation(""));

        assertThat(pair._2, is("oops"));
    }

    @Test
    void invokingWithUnregisteredRequestTypeInvokesErrorCallbackIfOnlyThisIsProvided() throws Exception {
        sut.onProcessingException((rpci, error) -> error.getMessage());

        Pair<MyRpcInvocation, String> pair = sut.apply(new MyRpcInvocation(""));

        assertThat(pair._2, containsString("not supported"));
    }

    @Test
    void registeringTheSameRequestTypeAgainThrows() {
        sut.register(String.class, s -> null);
        IllegalArgumentException iae = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.register(String.class, s -> null));
        assertThat(iae.getMessage(), Matchers.containsString("already registered"));
    }

    @Test
    void registeredFunctionsGetProperlyInvoked() throws Exception {
        sut.register(String.class, x -> "String");
        sut.register(Integer.class, x -> "Int");
        sut.register(Double.class, x -> "Double");

        Pair<MyRpcInvocation, String> pair = sut.apply(new MyRpcInvocation(""));
        assertThat(pair._2, is("String"));
        pair = sut.apply(new MyRpcInvocation(Integer.valueOf(1)));
        assertThat(pair._2, is("Int"));
        pair = sut.apply(new MyRpcInvocation(2));
        assertThat(pair._2, is("Int"));
        pair = sut.apply(new MyRpcInvocation(Double.valueOf(3.0)));
        assertThat(pair._2, is("Double"));
        pair = sut.apply(new MyRpcInvocation(3.0));
        assertThat(pair._2, is("Double"));
    }


    private class MyRpcInvocation extends RpcInvocation<Object, Void, String, Void> {
        private String reply;
        private Throwable error;

        public MyRpcInvocation(Object request) {
            super(request, null, null, null);
        }
    }
}