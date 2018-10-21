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
import ch.squaredesk.nova.tuples.Tuple3;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultMarshallerRegistryForStringAsTransportTypeTest {
    private DefaultMarshallerRegistryForStringAsTransportType sut = new DefaultMarshallerRegistryForStringAsTransportType();

    static Stream<Tuple3<Class, Object, String>> marshallerTestDefs() {
        return Stream.of(
            new Tuple3<>(String.class, "4", "4"),
            new Tuple3<>(Integer.class, 4, "4"),
            new Tuple3<>(Double.class, 4.0, "4.0"),
            new Tuple3<>(BigDecimal.class, BigDecimal.ONE, "1"),
            new Tuple3<>(MyClass.class, new MyClass("xxx"), "{\"field\":\"xxx\"}")
        );
    }
    @ParameterizedTest
    @MethodSource("marshallerTestDefs")
    void registryInitializedWithDefaultMarshaller(Tuple3<Class, Object, String> testDef) throws Exception {
        MessageMarshaller<Object, String> marshaller = sut.getMarshallerForMessageType(testDef._1);

        String marshalled = marshaller.marshal(testDef._2);

        assertThat(marshalled, is(testDef._3));
    }

    static Stream<Tuple3<Class, String, Object>> unmarshallerTestDefs() {
        return Stream.of(
            new Tuple3<>(String.class, "4", "4"),
            new Tuple3<>(Integer.class, "4", 4),
            new Tuple3<>(Double.class, "4", 4.0),
            new Tuple3<>(BigDecimal.class, "1", BigDecimal.ONE),
            new Tuple3<>(MyClass.class, "{\"field\":\"xxx\"}", new MyClass("xxx"))
        );
    }
    @ParameterizedTest
    @MethodSource("unmarshallerTestDefs")
    void registryInitializedWithDefaultUnmarshaller(Tuple3<Class, String, Object> testDef) throws Exception {
        MessageUnmarshaller<String, ?> unmarshaller = sut.getUnmarshallerForMessageType(testDef._1);

        Object unmarshalled = unmarshaller.unmarshal(testDef._2);

        assertThat(unmarshalled, is(testDef._3));
    }

    @Test
    void registryDoesNotSupportUnmarshallerForObject() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> sut.getUnmarshallerForMessageType(Object.class));
        assertThat(ex.getMessage(), Matchers.endsWith("not supported"));
    }

    @Test
    void customDefaultMarshallerCanBeSet() throws Exception {
        sut.setDefaultMarshaller(Integer.class, i -> "An integer");

        String marshalled = sut.getMarshallerForMessageType(Integer.class).marshal(12);

        assertThat(marshalled, is("An integer"));
    }

    @Test
    void customDefaultUnmarshallerCanBeSet() throws Exception {
        sut.setDefaultUnmarshaller(Integer.class, s -> 23);

        Integer unmarshalled = sut.getUnmarshallerForMessageType(Integer.class).unmarshal("12");

        assertThat(unmarshalled, is(23));
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