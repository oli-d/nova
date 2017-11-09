package ch.squaredesk.nova.comm.websockets.annotation;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static ch.squaredesk.nova.comm.websockets.annotation.BeanExaminer.methodSignatureValidForMessageHandler;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

class BeanExaminerTest {
    @Test
    void marshallerInputTypeCheckWorks() throws Exception {
        MessageMarshaller<Integer, String> integerMarshaller = new MessageMarshaller<Integer, String>() {
            @Override
            public String marshal(Integer integer) throws Exception {
                return "Hallo";
            }
        };
        assertThat(BeanExaminer.marshallerAcceptsType(integerMarshaller, Integer.class), is(true));
        assertThat(BeanExaminer.marshallerAcceptsType(integerMarshaller, String.class), is(false));
    }

    @Test
    void unmarshallerOutputTypeCheckWorks() throws Exception {
        MessageUnmarshaller<String, Integer> integerUnmarshaller = new MessageUnmarshaller<String, Integer>() {
            @Override
            public Integer unmarshal(String s) throws Exception {
                return null;
            }
        };
        assertThat(BeanExaminer.unmarshallerReturnsType(integerUnmarshaller, Integer.class), is(true));
        assertThat(BeanExaminer.unmarshallerReturnsType(integerUnmarshaller, String.class), is(false));
    }

    @Test
    void handlerMethodSignatureCheckWorks() throws Exception {
        class SomeClass {
            void validSignature (Integer message, WebSocket<Integer> webSocket) {}
            void invalidSignatureMissingMessageParam (WebSocket<Integer> webSocket) {}
            void invalidSignatureMissingWebSocketParam (Integer message) {}
            String invalidSignatureWrongReturnType(Integer message, WebSocket<Integer> webSocket) { return "Hallo"; }
        }

        assertThat(
                methodSignatureValidForMessageHandler(
                        SomeClass.class.getDeclaredMethod("validSignature", Integer.class, WebSocket.class)),
                is(true));
        assertThat(
                methodSignatureValidForMessageHandler(
                        SomeClass.class.getDeclaredMethod("invalidSignatureMissingMessageParam", WebSocket.class)),
                is(false));
        assertThat(
                methodSignatureValidForMessageHandler(
                        SomeClass.class.getDeclaredMethod("invalidSignatureMissingWebSocketParam", Integer.class)),
                is(false));
        assertThat(
                methodSignatureValidForMessageHandler(
                        SomeClass.class.getDeclaredMethod("invalidSignatureWrongReturnType", Integer.class, WebSocket.class)),
                is(false));
    }

}