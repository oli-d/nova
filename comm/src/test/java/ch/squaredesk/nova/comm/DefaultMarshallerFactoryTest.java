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

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static ch.squaredesk.nova.comm.DefaultMarshallerFactory.getMarshallerForMessageType;
import static ch.squaredesk.nova.comm.DefaultMarshallerFactory.getUnmarshallerForMessageType;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultMarshallerFactoryTest {
    @Test
    void properMessageUnmarshallerCreatedForMessageHandler() throws Exception {
        MessageUnmarshaller unmarshaller = getUnmarshallerForMessageType(String.class);
        assertThat(unmarshaller.unmarshal("4"), is("4"));
        unmarshaller = getUnmarshallerForMessageType(Integer.class);
        assertThat(unmarshaller.unmarshal("4"), is(4));
        unmarshaller = getUnmarshallerForMessageType(Double.class);
        assertThat(unmarshaller.unmarshal("4"), is(4.0));
        unmarshaller = getUnmarshallerForMessageType(BigDecimal.class);
        assertThat(unmarshaller.unmarshal("1"), is(BigDecimal.ONE));
        unmarshaller = getUnmarshallerForMessageType(MyClass.class);
        assertThat(unmarshaller.unmarshal("{ \"field\":\"Hallo\"}"), is(new MyClass("Hallo")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> getUnmarshallerForMessageType(Object.class) );
        assertThat(ex.getMessage(), Matchers.endsWith("not supported"));
    }

    @Test
    void properMessageMarshallerCreatedForMessageHandler() throws Exception {
        MessageMarshaller marshaller = getMarshallerForMessageType(String.class);
        assertThat(marshaller.marshal("4"), is("4"));
        marshaller = getMarshallerForMessageType(Integer.class);
        assertThat(marshaller.marshal(4), is("4"));
        marshaller = getMarshallerForMessageType(Double.class);
        assertThat(marshaller.marshal(4.0), is("4.0"));
        marshaller = getMarshallerForMessageType(Double.class);
        assertThat(marshaller.marshal(BigDecimal.ONE), is("1"));
        marshaller = getMarshallerForMessageType(MyClass.class);
        assertThat(marshaller.marshal(new MyClass("xxx")), is("{\"field\":\"xxx\"}"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> getMarshallerForMessageType(Object.class) );
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