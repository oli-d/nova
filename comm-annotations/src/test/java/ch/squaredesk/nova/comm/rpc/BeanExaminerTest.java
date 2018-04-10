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
            public String processRequest(RpcInvocation<String, Void, String, Void> rpcInvocation) {
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
        class TwoArgs {
            @OnRpcInvocation(String.class)
            public String processRequest(String s0, String s) {
                return "";
            }
        }
        class WrongArgType {
            @OnRpcInvocation(String.class)
            public String processRequest(Integer i) {
                return "";
            }
        }
        class NotPublic {
            @OnRpcInvocation(String.class)
            private String processRequest(String s) {
                return "";
            }
        }
        class NoReturnValue1 {
            @OnRpcInvocation(String.class)
            public void processRequest(String s) {
            }
        }
        class NoReturnValue2 {
            @OnRpcInvocation(String.class)
            public Void processRequest(String s) {
                return null;
            }
        }

        return Stream.of(new NoArgs(), new TwoArgs(), new WrongArgType(),
                new NotPublic(), new NoReturnValue1(), new NoReturnValue2());
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
            public void processStringRequest(RpcInvocation<String, ?, ?,?> i) {
            }

            @OnRpcInvocation(Double.class)
            public void foo(RpcInvocation<Double, ?, ?,?> i) {
            }
        }

        RpcRequestHandlerDescription[] descriptions = sut.examine(new MyClass());
        assertThat(descriptions.length, is(2));
    }
}