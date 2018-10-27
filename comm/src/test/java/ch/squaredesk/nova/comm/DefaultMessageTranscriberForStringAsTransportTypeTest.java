package ch.squaredesk.nova.comm;

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
    void defaultTranscribersAreSet() throws Exception {
        assertThat(sut.transcribeOutgoingMessage("a String"), is("a String"));
        assertThat(sut.transcribeOutgoingMessage(1), is("1"));
        assertThat(sut.transcribeOutgoingMessage(1.0), is("1.0"));
        assertThat(sut.transcribeOutgoingMessage(new BigDecimal("1.0")), is("1.0"));
        assertThat(sut.transcribeOutgoingMessage(new MessageTranscriberTest.MyClass("xxx")), is("{\"field\":\"xxx\"}"));

        assertThat(sut.transcribeIncomingMessage("a String", String.class), is("a String"));
        assertThat(sut.transcribeIncomingMessage("1", Integer.class), is(1));
        assertThat(sut.transcribeIncomingMessage("1", Double.class), is(1.0));
        assertThat(sut.transcribeIncomingMessage("1", BigDecimal.class), is(BigDecimal.ONE));
        assertThat(sut.transcribeIncomingMessage("{\"field\":\"xxx\"}", MessageTranscriberTest.MyClass.class), is(new MessageTranscriberTest.MyClass("xxx")));
    }

    @Test
    void typeSpecificTranscribersCanBeDeegistered() throws Exception {
        sut.registerClassSpecificTranscribers(Integer.class, s -> "Integer", s -> 5);
        assertThat(sut.transcribeIncomingMessage("a String", Integer.class), is(5));
        assertThat(sut.transcribeOutgoingMessage(1), is("Integer"));

        sut.registerClassSpecificTranscribers(Integer.class, null, null);
        assertThat(sut.transcribeIncomingMessage("1", Integer.class), is(1));
        assertThat(sut.transcribeOutgoingMessage(1), is("1"));
    }


}