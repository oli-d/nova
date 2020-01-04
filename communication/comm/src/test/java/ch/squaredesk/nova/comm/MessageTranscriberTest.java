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

package ch.squaredesk.nova.comm;

import io.reactivex.functions.Function;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MessageTranscriberTest {
    private MessageTranscriber<String> sut;

    @BeforeEach
    void setUp() {
        sut = new MessageTranscriber<>(null, null);
    }

    @Test
    void typeSpecificTranscribersCanBeRegistered() {
        Function<String, String> outgoingStringFunction = s -> "Outgoing";
        Function<String, String> incomingStringFunction = s -> "Incoming";
        Function<Integer, String> outgoingIntegerFunction = s -> "Integer";
        Function<String, Integer> incomingIntegerFunction = s -> 1;

        sut.registerClassSpecificTranscribers(String.class, outgoingStringFunction, incomingStringFunction);
        sut.registerClassSpecificTranscribers(Integer.class, outgoingIntegerFunction, incomingIntegerFunction);

        assertThat(sut.getOutgoingMessageTranscriber(String.class), sameInstance(outgoingStringFunction));
        assertThat(sut.getIncomingMessageTranscriber(String.class), sameInstance(incomingStringFunction));
        assertThat(sut.getOutgoingMessageTranscriber(Integer.class), sameInstance(outgoingIntegerFunction));
        assertThat(sut.getIncomingMessageTranscriber(Integer.class), sameInstance(incomingIntegerFunction));
    }

    @Test
    void typeSpecificHandlersCanBeNull() throws Exception {
        sut.registerClassSpecificTranscribers(String.class, null, null);
    }

    @Test
    void typeSpecificTranscribersCannotBeRegisteredWithNullClass() throws Exception {
        assertThrows(NullPointerException.class,
                () -> sut.registerClassSpecificTranscribers(null, s -> "Outgoing", s -> "Incoming"));
    }

    @Test
    void typeSpecificTranscribersCannotBeRegisteredForObjectClass() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> sut.registerClassSpecificTranscribers(Object.class, Object::toString, s -> new Object()));
        assertThat(ex.getMessage(), Matchers.endsWith("not supported"));
    }

    @Test
    void typeSpecificTranscribersCanBeDeregistered() {
        sut.registerClassSpecificTranscribers(Integer.class, s -> "Integer", s -> 5);
        assertNotNull(sut.getOutgoingMessageTranscriber(Integer.class));
        assertNotNull(sut.getIncomingMessageTranscriber(Integer.class));

        sut.registerClassSpecificTranscribers(Integer.class, s -> "Integer", null);
        assertNotNull(sut.getOutgoingMessageTranscriber(Integer.class));
        assertThrows(RuntimeException.class, () -> sut.getIncomingMessageTranscriber(Integer.class));

        sut.registerClassSpecificTranscribers(Integer.class, null, s -> 5);
        assertThrows(RuntimeException.class, () -> sut.getOutgoingMessageTranscriber(Integer.class));
        assertNotNull(sut.getIncomingMessageTranscriber(Integer.class));

        sut.registerClassSpecificTranscribers(Integer.class, null, null);
        assertThrows(RuntimeException.class, () -> sut.getOutgoingMessageTranscriber(Integer.class));
        assertThrows(RuntimeException.class, () -> sut.getIncomingMessageTranscriber(Integer.class));
    }
}