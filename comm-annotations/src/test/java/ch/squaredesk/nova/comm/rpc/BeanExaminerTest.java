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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BeanExaminerTest {
    private BeanExaminer sut = new BeanExaminer();

    @Test
    void noAnnotationsReturnEmptyArray() {
        class MissingAnnotation {
            public String processRequest(String request, RpcCompletor<String, Void> rpcCompletor) {
                return "";
            }
        }

        assertThat(sut.examine(new Object()).length, is(0));
        assertThat(sut.examine(new MissingAnnotation()).length, is(0));
    }

    static Stream<Object> invalidHandlerDeclarations() {
        class NoArgs {
            @OnRpcInvocation(String.class)
            public String processRequest() {
                return "";
            }
        }
        class FirstArgTypeWrong {
            @OnRpcInvocation(String.class)
            public String processRequest(Integer i0, RpcCompletor<String, ?> c) {
                return "";
            }
        }
        class SecondArgTypeWrong {
            @OnRpcInvocation(String.class)
            public String processRequest(String s0, String s) {
                return "";
            }
        }
        class TooManyArgs {
            @OnRpcInvocation(String.class)
            public String processRequest(String s, RpcCompletor<String, ?> c, String s2) {
                return "";
            }
        }
        class NotPublic {
            @OnRpcInvocation(String.class)
            private String processRequest(String s, RpcCompletor<String, ?> c) {
                return "";
            }
        }
        class HasReturnValue {
            @OnRpcInvocation(String.class)
            public String processRequest(String s, RpcCompletor<String, ?> c) {
                return null;
            }
        }

        return Stream.of(new NoArgs(), new FirstArgTypeWrong(), new SecondArgTypeWrong(),
                new NotPublic(), new TooManyArgs(), new HasReturnValue());
    }

    @ParameterizedTest
    @MethodSource("invalidHandlerDeclarations")
    void invalidDeclarationThrows(Object beanToExamine) {
        assertThrows(IllegalArgumentException.class, () -> sut.examine(beanToExamine));
    }

    @Test
    void handlersProperlyDetected() {
        class MyClass {
            @OnRpcInvocation(String.class)
            public void processStringRequest(String s, RpcInvocation<String, ?, ?,?> i) {
            }

            @OnRpcInvocation(Double.class)
            public void foo(Double d, RpcInvocation<Double, ?, ?,?> i) {
            }
        }

        RpcRequestHandlerDescription[] descriptions = sut.examine(new MyClass());
        assertThat(descriptions.length, is(2));
    }
}