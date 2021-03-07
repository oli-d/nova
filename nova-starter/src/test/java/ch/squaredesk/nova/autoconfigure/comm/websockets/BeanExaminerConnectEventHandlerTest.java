/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.websockets;

public class BeanExaminerConnectEventHandlerTest {
    private BeanExaminer sut;

/*
    @BeforeEach
    void setup() {
        sut = new BeanExaminer(new DefaultMessageTranscriberForStringAsTransportType());
    }

    @Test
    void beanExaminerThrowsForMethodWithInvalidSignature() throws Exception {
        class EmptyClass {
        }
        assertThat(sut.onConnectHandlersIn(new EmptyClass()).size(), is(0));

        class AnotherEmptyClass {
            public void handle (String msg, WebSocket webSocket) {}
        }
        assertThat(sut.onConnectHandlersIn(new AnotherEmptyClass()).size(), is(0));

        class ValidClassAllDefaults {
            @OnConnect("hallo")
            void validSignature(WebSocket webSocket) {
            }
        }
        Collection<EventHandlerEndpointDescriptor> endpoints = sut.onConnectHandlersIn(new ValidClassAllDefaults());
        assertThat(endpoints.size(), is(1));
        EventHandlerEndpointDescriptor descriptor = endpoints.iterator().next();
        assertThat(descriptor.destination, is("hallo"));
        assertThat(descriptor.captureTimings, is(true));

        class ValidClass {
            @OnConnect(value ="hallo2", captureTimings = false)
            void validSignature(WebSocket webSocket) {
            }
        }
        endpoints = sut.onConnectHandlersIn(new ValidClass());
        descriptor = endpoints.iterator().next();
        assertThat(endpoints.size(), is(1));
        assertThat(descriptor.destination, is("hallo2"));
        assertThat(descriptor.captureTimings, is(false));

        class InvalidClassMissingWebSocketParam {
            @OnConnect("x")
            void invalidSignatureMissingWebSocketParam() {
            }
        }
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onConnectHandlersIn(new InvalidClassMissingWebSocketParam()));
        assertTrue(ex.getMessage().contains("annotated with @OnConnect has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassMissingWebSocketParam.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureMissingWebSocketParam"));

        class InvalidClassWrongReturnType {
            @OnConnect("x")
            String invalidSignatureWrongReturnType(WebSocket webSocket) {
                return "Hallo";
            }
        }
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sut.onConnectHandlersIn(new InvalidClassWrongReturnType()));
        assertTrue(ex.getMessage().contains("annotated with @OnConnect has an invalid signature"));
        assertTrue(ex.getMessage().contains(InvalidClassWrongReturnType.class.getName()));
        assertTrue(ex.getMessage().contains("invalidSignatureWrongReturnType"));
    }

 */
}