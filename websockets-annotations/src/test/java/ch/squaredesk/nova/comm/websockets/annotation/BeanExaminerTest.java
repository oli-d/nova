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

package ch.squaredesk.nova.comm.websockets.annotation;

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import io.reactivex.BackpressureStrategy;
import io.reactivex.functions.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanExaminerTest {
    private BeanExaminer sut;

    @BeforeEach
    void setup() {
        sut = new BeanExaminer(new DefaultMessageTranscriberForStringAsTransportType());
    }

    @Test
    void beanExaminerThrowsForMethodWithInvalidSignature() throws Exception {
        class EmptyClass {
        }
        assertThat(sut.websocketEndpointsIn(new EmptyClass()).length, is(0));

        class AnotherEmptyClass {
            public void handle (String msg, WebSocket webSocket) {}
        }
        assertThat(sut.websocketEndpointsIn(new AnotherEmptyClass()).length, is(0));

        class ValidClassAllDefaults {
            @OnMessage("hallo")
            void validSignature(Integer message, WebSocket webSocket) {
            }
        }
        EndpointDescriptor[] endpoints = sut.websocketEndpointsIn(new ValidClassAllDefaults());
        assertThat(endpoints.length, is(1));
        assertThat(endpoints[0].destination, is("hallo"));
        assertThat(endpoints[0].messageType, is(Integer.class));
        assertThat(endpoints[0].backpressureStrategy, is(BackpressureStrategy.BUFFER));
        assertThat(endpoints[0].captureTimings, is(true));

        class ValidClass {
            @OnMessage(value ="hallo2", backpressureStrategy = BackpressureStrategy.DROP, captureTimings = false)
            void validSignature(Integer message, WebSocket webSocket) {
            }
        }
        endpoints = sut.websocketEndpointsIn(new ValidClass());
        assertThat(endpoints.length, is(1));
        assertThat(endpoints[0].destination, is("hallo2"));
        assertThat(endpoints[0].backpressureStrategy, is(BackpressureStrategy.DROP));
        assertThat(endpoints[0].captureTimings, is(false));

        class InvalidClassMissingMessageParam {
            @OnMessage("x")
            void invalidSignatureMissingMessageParam(WebSocket webSocket) {
            }
        }
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new InvalidClassMissingMessageParam()));
        assertTrue(ex.getMessage().contains("annotated with @OnMessage has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassMissingMessageParam.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureMissingMessageParam"));

        class InvalidClassMissingWebSocketParam {
            @OnMessage("x")
            void invalidSignatureMissingWebSocketParam(Integer message) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new InvalidClassMissingWebSocketParam()));
        assertTrue(ex.getMessage().contains("annotated with @OnMessage has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassMissingWebSocketParam.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureMissingWebSocketParam"));

        class InvalidClassWrongReturnType {
            @OnMessage("x")
            String invalidSignatureWrongReturnType(Integer message, WebSocket webSocket) {
                return "Hallo";
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new InvalidClassWrongReturnType()));
        assertTrue(ex.getMessage().contains("annotated with @OnMessage has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassWrongReturnType.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureWrongReturnType"));
    }

    @Test
    void beanExaminerCreatesEndpointForCustomMarshaller() throws Exception {
        class SomeClass {
            @OnMessage(value = "x", messageMarshallerClassName = "ch.squaredesk.nova.comm.websockets.annotation.BeanExaminerTest$StringMarshaller")
            void handle(String message, WebSocket webSocket) {
            }
        }
        EndpointDescriptor[] endpoints = sut.websocketEndpointsIn(new SomeClass());
        assertThat(endpoints.length, is(1));
        assertThat(endpoints[0].destination, is("x"));
        assertThat(endpoints[0].messageType, is(String.class));
        assertThat(endpoints[0].backpressureStrategy, is(BackpressureStrategy.BUFFER));
        assertThat(endpoints[0].captureTimings, is(true));
    }

    @Test
    void beanExaminerCreatesEndpointForCustomUnmarshaller() throws Exception {
        class SomeClass {
            @OnMessage(value = "x", messageUnmarshallerClassName = "ch.squaredesk.nova.comm.websockets.annotation.BeanExaminerTest$StringUnmarshaller")
            void handle(String message, WebSocket webSocket) {
            }
        }
        EndpointDescriptor[] endpoints = sut.websocketEndpointsIn(new SomeClass());
        assertThat(endpoints.length, is(1));
        assertThat(endpoints[0].destination, is("x"));
        assertThat(endpoints[0].messageType, is(String.class));
        assertThat(endpoints[0].backpressureStrategy, is(BackpressureStrategy.BUFFER));
        assertThat(endpoints[0].captureTimings, is(true));
    }

    @Test
    void beanExaminerThrowsForInvalidCustomMarshaller() throws Exception {
        class InvalidMarshallerClassName {
            @OnMessage(value = "x", messageMarshallerClassName = "bla bla")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new InvalidMarshallerClassName()));
        assertThat(ex.getMessage(), is("Unable to load class bla bla"));

        class MarshallerClassNameThatCantBeInstantiated {
            @OnMessage(value = "x", messageMarshallerClassName = "java.lang.Double")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new MarshallerClassNameThatCantBeInstantiated()));
        assertThat(ex.getMessage(), is("Unable to instantiate class java.lang.Double"));

        class MarshallerClassThatIsNotAnUnmarshaller {
            @OnMessage(value = "x", messageMarshallerClassName = "java.util.ArrayList")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new MarshallerClassThatIsNotAnUnmarshaller()));
        assertThat(ex.getMessage(), is("Class java.util.ArrayList is not a valid MessageMarshaller"));

        class MarshallerClassThatDoesNotMatchMessageType {
            @OnMessage(value = "x", messageMarshallerClassName = "ch.squaredesk.nova.comm.websockets.annotation.BeanExaminerTest$StringMarshaller")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new MarshallerClassThatDoesNotMatchMessageType()));
        assertThat(ex.getMessage(), is("Class ch.squaredesk.nova.comm.websockets.annotation.BeanExaminerTest$StringMarshaller is not a valid MessageMarshaller for method handle"));
    }

    @Test
    void beanExaminerThrowsForInvalidCustomUnmarshaller() throws Exception {
        class InvalidUnmarshallerClassName {
            @OnMessage(value = "x", messageUnmarshallerClassName = "bla bla")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new InvalidUnmarshallerClassName()));
        assertThat(ex.getMessage(), is("Unable to load class bla bla"));

        class UnmarshallerClassNameThatCantBeInstantiated {
            @OnMessage(value = "x", messageUnmarshallerClassName = "java.lang.Double")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new UnmarshallerClassNameThatCantBeInstantiated()));
        assertThat(ex.getMessage(), is("Unable to instantiate class java.lang.Double"));

        class UnmarshallerClassThatIsNotAnUnmarshaller {
            @OnMessage(value = "x", messageUnmarshallerClassName = "java.util.ArrayList")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new UnmarshallerClassThatIsNotAnUnmarshaller()));
        assertThat(ex.getMessage(), is("Class java.util.ArrayList is not a valid MessageUnmarshaller"));

        class UnmarshallerClassThatDoesNotMatchMessageType {
            @OnMessage(value = "x", messageUnmarshallerClassName = "ch.squaredesk.nova.comm.websockets.annotation.BeanExaminerTest$StringUnmarshaller")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.websocketEndpointsIn(new UnmarshallerClassThatDoesNotMatchMessageType()));
        assertThat(ex.getMessage(), is("Class ch.squaredesk.nova.comm.websockets.annotation.BeanExaminerTest$StringUnmarshaller is not a valid MessageUnmarshaller for method handle"));
    }

    public static class StringMarshaller implements Function<String, String> {
        @Override
        public String apply(String s) throws Exception {
            return s;
        }
    }

    public static class StringUnmarshaller implements Function<String, String> {
        @Override
        public String apply(String s) throws Exception {
            return s;
        }
    }
}