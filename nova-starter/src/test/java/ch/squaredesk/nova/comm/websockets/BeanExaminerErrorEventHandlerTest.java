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
import ch.squaredesk.nova.comm.websockets.annotation.OnError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanExaminerErrorEventHandlerTest {
    private BeanExaminer sut;

    @BeforeEach
    void setup() {
        sut = new BeanExaminer(new DefaultMessageTranscriberForStringAsTransportType());
    }

    @Test
    void beanExaminerThrowsForMethodWithInvalidSignature() throws Exception {
        class EmptyClass {
        }
        assertThat(sut.onErrorHandlersIn(new EmptyClass()).size(), is(0));

        class AnotherEmptyClass {
            public void handle (WebSocket webSocket, Throwable error) {}
        }
        assertThat(sut.onErrorHandlersIn(new AnotherEmptyClass()).size(), is(0));

        class ValidClassAllDefaults {
            @OnError("hallo")
            void validSignature(WebSocket webSocket, Throwable error) {
            }
        }
        Collection<EventHandlerEndpointDescriptor> endpoints = sut.onErrorHandlersIn(new ValidClassAllDefaults());
        assertThat(endpoints.size(), is(1));
        EventHandlerEndpointDescriptor descriptor = endpoints.iterator().next();
        assertThat(descriptor.destination, is("hallo"));
        assertThat(descriptor.captureTimings, is(true));

        class ValidClass {
            @OnError(value ="hallo2", captureTimings = false)
            void validSignature(WebSocket webSocket, Throwable error) {
            }
        }
        endpoints = sut.onErrorHandlersIn(new ValidClass());
        descriptor = endpoints.iterator().next();
        assertThat(endpoints.size(), is(1));
        assertThat(descriptor.destination, is("hallo2"));
        assertThat(descriptor.captureTimings, is(false));

        class InvalidClassMissingWebSocketParam {
            @OnError("x")
            void invalidSignatureMissingWebSocketParam(String s, Throwable error) {
            }
        }
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onErrorHandlersIn(new InvalidClassMissingWebSocketParam()));
        assertTrue(ex.getMessage().contains("annotated with @OnError has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassMissingWebSocketParam.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureMissingWebSocketParam"));

        class InvalidClassMissingCloseReasonParam {
            @OnError("x")
            void invalidSignatureMissingWebSocketParam(WebSocket webSocket, String s) {
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onErrorHandlersIn(new InvalidClassMissingWebSocketParam()));
        assertTrue(ex.getMessage().contains("annotated with @OnError has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassMissingWebSocketParam.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureMissingWebSocketParam"));

        class InvalidClassWrongReturnType {
            @OnError("x")
            String invalidSignatureWrongReturnType(WebSocket webSocket, Throwable error) {
                return "Hallo";
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onErrorHandlersIn(new InvalidClassWrongReturnType()));
        assertTrue(ex.getMessage().contains("annotated with @OnError has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassWrongReturnType.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureWrongReturnType"));
    }

}