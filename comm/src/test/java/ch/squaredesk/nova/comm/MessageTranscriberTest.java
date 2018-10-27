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

package ch.squaredesk.nova.comm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MessageTranscriberTest {
    private MessageTranscriber<String> sut;

    @BeforeEach
    void setUp() {
        sut = new MessageTranscriber<>(null, null);
    }

    @Test
    void typeSpecificTranscribersCanBeRegistered() throws Exception {
        sut.registerClassSpecificTranscribers(String.class, s -> "Outgoing", s -> "Incoming");
        sut.registerClassSpecificTranscribers(Integer.class, s -> "Integer", s -> 1);

        assertThat(sut.transcribeOutgoingMessage("a String"), is("Outgoing"));
        assertThat(sut.transcribeIncomingMessage("a String", String.class), is("Incoming"));
        assertThat(sut.transcribeOutgoingMessage(1), is("Integer"));
        assertThat(sut.transcribeIncomingMessage("a String", Integer.class), is(1));
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

    public static class MyClass {
        public final String field;

        @JsonCreator
        public MyClass(@JsonProperty("field") String  field) {
            this.field = field;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyClass myClass = (MyClass) o;

            return field != null ? field.equals(myClass.field) : myClass.field == null;
        }

        @Override
        public int hashCode() {
            return field != null ? field.hashCode() : 0;
        }
    }
}