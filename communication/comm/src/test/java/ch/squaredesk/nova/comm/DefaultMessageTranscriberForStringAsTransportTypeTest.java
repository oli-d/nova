package ch.squaredesk.nova.comm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

class DefaultMessageTranscriberForStringAsTransportTypeTest {
    private DefaultMessageTranscriberForStringAsTransportType sut;

    @BeforeEach
    void setUp() {
        sut = new DefaultMessageTranscriberForStringAsTransportType();
    }

    @Test
    void specificObjectMapperCanBeUsed() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configOverride(BigDecimal.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        DefaultMessageTranscriberForStringAsTransportType sut = new DefaultMessageTranscriberForStringAsTransportType(objectMapper);

        String valueAsString = sut.getOutgoingMessageTranscriber(BigDecimal.class).apply(new BigDecimal("234"));
        assertThat(valueAsString, is("\"234\""));
    }

    @Test
    void defaultTranscribersAreSet() throws Exception {
        assertThat(sut.getOutgoingMessageTranscriber(String.class).apply("a String"), is("a String"));
        assertThat(sut.getOutgoingMessageTranscriber(Integer.class).apply(1), is("1"));
        assertThat(sut.getOutgoingMessageTranscriber(Double.class).apply(1.0), is("1.0"));
        assertThat(sut.getOutgoingMessageTranscriber(BigDecimal.class).apply(new BigDecimal("1.0")), is("1.0"));
        assertThat(sut.getOutgoingMessageTranscriber(MyClass.class).apply(new MyClass("xxx")), is("{\"field\":\"xxx\"}"));

        assertThat(sut.getIncomingMessageTranscriber(String.class).apply("a String"), is("a String"));
        assertThat(sut.getIncomingMessageTranscriber(Integer.class).apply("1"), is(1));
        assertThat(sut.getIncomingMessageTranscriber(Double.class).apply("1"), is(1.0));
        assertThat(sut.getIncomingMessageTranscriber(BigDecimal.class).apply("1"), is(BigDecimal.ONE));
        assertThat(sut.getIncomingMessageTranscriber(MyClass.class).apply("{\"field\":\"xxx\"}"), is(new MyClass("xxx")));
    }

    public static class MyClass {
        public final String field;

        @JsonCreator
        public MyClass(@JsonProperty("field") String field) {
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