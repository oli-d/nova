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

package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.websockets.annotation.OnMessage;
import io.reactivex.BackpressureStrategy;
import io.reactivex.functions.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanExaminerOnMessageHandlerTest {
    private BeanExaminer sut;

    @BeforeEach
    void setup() {
        sut = new BeanExaminer(new DefaultMessageTranscriberForStringAsTransportType());
    }

    @Test
    void beanExaminerThrowsForMethodWithInvalidSignature() throws Exception {
        class EmptyClass {
        }
        assertThat(sut.onMessageEndpointsIn(new EmptyClass()).size(), is(0));

        class AnotherEmptyClass {
            public void handle (String msg, WebSocket webSocket) {}
        }
        assertThat(sut.onMessageEndpointsIn(new AnotherEmptyClass()).size(), is(0));

        class ValidClassAllDefaults {
            @OnMessage("hallo")
            void validSignature(Integer message, WebSocket webSocket) {
            }
        }
        Collection<OnMessageHandlerEndpointDescriptor> endpoints = sut.onMessageEndpointsIn(new ValidClassAllDefaults());
        assertThat(endpoints.size(), is(1));
        OnMessageHandlerEndpointDescriptor descriptor = endpoints.iterator().next();
        assertThat(descriptor.destination, is("hallo"));
        assertTrue(descriptor.messageType.isAssignableFrom(Integer.class));
        assertThat(descriptor.backpressureStrategy, is(BackpressureStrategy.BUFFER));
        assertThat(descriptor.captureTimings, is(true));

        class ValidClass {
            @OnMessage(value ="hallo2", backpressureStrategy = BackpressureStrategy.DROP, captureTimings = false)
            void validSignature(Integer message, WebSocket webSocket) {
            }
        }
        endpoints = sut.onMessageEndpointsIn(new ValidClass());
        descriptor = endpoints.iterator().next();
        assertThat(endpoints.size(), is(1));
        assertThat(descriptor.destination, is("hallo2"));
        assertThat(descriptor.backpressureStrategy, is(BackpressureStrategy.DROP));
        assertThat(descriptor.captureTimings, is(false));

        class InvalidClassMissingMessageParam {
            @OnMessage("x")
            void invalidSignatureMissingMessageParam(WebSocket webSocket) {
            }
        }
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new InvalidClassMissingMessageParam()));
        assertTrue(ex.getMessage().contains("annotated with @OnMessage has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassMissingMessageParam.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureMissingMessageParam"));

        class InvalidClassMissingWebSocketParam {
            @OnMessage("x")
            void invalidSignatureMissingWebSocketParam(Integer message) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new InvalidClassMissingWebSocketParam()));
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
                () -> sut.onMessageEndpointsIn(new InvalidClassWrongReturnType()));
        assertTrue(ex.getMessage().contains("annotated with @OnMessage has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassWrongReturnType.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureWrongReturnType"));
    }

    @Test
    void beanExaminerCreatesEndpointForCustomMarshaller() throws Exception {
        class SomeClass {
            @OnMessage(value = "x", messageMarshallerClassName = "ch.squaredesk.nova.comm.websockets.BeanExaminerOnMessageHandlerTest$StringMarshaller")
            void handle(String message, WebSocket webSocket) {
            }
        }
        Collection<OnMessageHandlerEndpointDescriptor> endpoints = sut.onMessageEndpointsIn(new SomeClass());
        assertThat(endpoints.size(), is(1));
        OnMessageHandlerEndpointDescriptor descriptor = endpoints.iterator().next();
        assertThat(descriptor.destination, is("x"));
        assertTrue(descriptor.messageType.isAssignableFrom(String.class));
        assertThat(descriptor.backpressureStrategy, is(BackpressureStrategy.BUFFER));
        assertThat(descriptor.captureTimings, is(true));
    }

    @Test
    void beanExaminerCreatesEndpointForCustomUnmarshaller() throws Exception {
        class SomeClass {
            @OnMessage(value = "x", messageUnmarshallerClassName = "ch.squaredesk.nova.comm.websockets.BeanExaminerOnMessageHandlerTest$StringUnmarshaller")
            void handle(String message, WebSocket webSocket) {
            }
        }
        Collection<OnMessageHandlerEndpointDescriptor> endpoints = sut.onMessageEndpointsIn(new SomeClass());
        assertThat(endpoints.size(), is(1));
        OnMessageHandlerEndpointDescriptor descriptor = endpoints.iterator().next();
        assertThat(descriptor.destination, is("x"));
        assertTrue(descriptor.messageType.isAssignableFrom(String.class));
        assertThat(descriptor.backpressureStrategy, is(BackpressureStrategy.BUFFER));
        assertThat(descriptor.captureTimings, is(true));
    }

    @Test
    void beanExaminerThrowsForInvalidCustomMarshaller() throws Exception {
        class InvalidMarshallerClassName {
            @OnMessage(value = "x", messageMarshallerClassName = "bla bla")
            void handle(Integer message, WebSocket webSocket) {
            }
        }

        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new InvalidMarshallerClassName()));
        assertThat(ex.getMessage(), is("Unable to load class bla bla"));

        class MarshallerClassNameThatCantBeInstantiated {
            @OnMessage(value = "x", messageMarshallerClassName = "java.lang.Double")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new MarshallerClassNameThatCantBeInstantiated()));
        assertThat(ex.getMessage(), is("Unable to instantiate class java.lang.Double"));

        class MarshallerClassThatIsNotAnUnmarshaller {
            @OnMessage(value = "x", messageMarshallerClassName = "java.util.ArrayList")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new MarshallerClassThatIsNotAnUnmarshaller()));
        assertThat(ex.getMessage(), is("Class java.util.ArrayList is not a valid MessageMarshaller"));

        class MarshallerClassThatDoesNotMatchMessageType {
            @OnMessage(value = "x", messageMarshallerClassName = "ch.squaredesk.nova.comm.websockets.BeanExaminerOnMessageHandlerTest$StringMarshaller")
            void handle(Integer message, WebSocket webSocket) {
            }
        }

        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new MarshallerClassThatDoesNotMatchMessageType()));

        assertThat(ex.getMessage(), is("Class ch.squaredesk.nova.comm.websockets.BeanExaminerOnMessageHandlerTest$StringMarshaller is not a valid MessageMarshaller for method handle"));
    }

    @Test
    void beanExaminerThrowsForInvalidCustomUnmarshaller() throws Exception {
        class InvalidUnmarshallerClassName {
            @OnMessage(value = "x", messageUnmarshallerClassName = "bla bla")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new InvalidUnmarshallerClassName()));
        assertThat(ex.getMessage(), is("Unable to load class bla bla"));

        class UnmarshallerClassNameThatCantBeInstantiated {
            @OnMessage(value = "x", messageUnmarshallerClassName = "java.lang.Double")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new UnmarshallerClassNameThatCantBeInstantiated()));
        assertThat(ex.getMessage(), is("Unable to instantiate class java.lang.Double"));

        class UnmarshallerClassThatIsNotAnUnmarshaller {
            @OnMessage(value = "x", messageUnmarshallerClassName = "java.util.ArrayList")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new UnmarshallerClassThatIsNotAnUnmarshaller()));
        assertThat(ex.getMessage(), is("Class java.util.ArrayList is not a valid MessageUnmarshaller"));

        class UnmarshallerClassThatDoesNotMatchMessageType {
            @OnMessage(value = "x", messageUnmarshallerClassName = "ch.squaredesk.nova.comm.websockets.BeanExaminerOnMessageHandlerTest$StringUnmarshaller")
            void handle(Integer message, WebSocket webSocket) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onMessageEndpointsIn(new UnmarshallerClassThatDoesNotMatchMessageType()));
        assertThat(ex.getMessage(), is("Class ch.squaredesk.nova.comm.websockets.BeanExaminerOnMessageHandlerTest$StringUnmarshaller is not a valid MessageUnmarshaller for method handle"));
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