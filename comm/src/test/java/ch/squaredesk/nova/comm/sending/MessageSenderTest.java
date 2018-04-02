/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;

import ch.squaredesk.nova.comm.TestTransportInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class MessageSenderTest {
    private MessageSender<String, String, String, TestTransportInfo> sut;
    private List<String> transportMessages;
    private List<OutgoingMessageMetaData<String, TestTransportInfo>> outgoingMessageMetaData;

    @BeforeEach
    void setUp() {
        this.sut = new MessageSender<String, String, String, TestTransportInfo>(s -> s, new Metrics()) {
            @Override
            protected Completable doSend(String transportMessage, OutgoingMessageMetaData<String, TestTransportInfo> outgoingMessageMetaData) {
                transportMessages.add(transportMessage);
                MessageSenderTest.this.outgoingMessageMetaData.add(outgoingMessageMetaData);
                return Completable.complete();
            }
        };
        transportMessages = new ArrayList<>();
        outgoingMessageMetaData = new ArrayList<>();
    }

    @Test
    void instanceCannotBeCreatedWithoutMarshaller() {
        Throwable t = assertThrows(NullPointerException.class,
                () -> new MessageSender<Object,Object, Object, Object>(null, new Metrics()) {
                    @Override
                    protected Completable doSend(Object transportMessage, OutgoingMessageMetaData messageSendingInfo) {
                        return null;
                    }
                });
        assertThat(t.getMessage(), containsString("messageMarshaller"));
    }

    @Test
    void sendingDoneWithoutTransportInfo() throws Exception {
        sut.sendMessage(
                "origin",
                "message",
                null).subscribe();

        assertThat(transportMessages.size(), is(1));
        assertThat(transportMessages, contains("message"));
        assertThat(outgoingMessageMetaData.size(), is(1));
        assertThat(outgoingMessageMetaData.get(0).destination, is("origin"));
        assertNull(outgoingMessageMetaData.get(0).transportSpecificInfo);
    }

    @Test
    void sendingDoneWithAdditionalTransportInfo() throws Exception {
        TestTransportInfo myTransportInfo = new TestTransportInfo("v1");
        sut.sendMessage(
                "origin",
                "message",
                myTransportInfo)
                .subscribe();

        assertThat(transportMessages.size(), is(1));
        assertThat(transportMessages, contains("message"));
        assertThat(outgoingMessageMetaData.size(), is(1));
        assertThat(outgoingMessageMetaData.get(0).destination, is("origin"));
        assertNotNull(outgoingMessageMetaData.get(0).transportSpecificInfo);
        assertThat(outgoingMessageMetaData.get(0).transportSpecificInfo,sameInstance(myTransportInfo));
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendingErrorTransportedToCompletable() {
        this.sut = new MessageSender<String, String, String, TestTransportInfo>(s -> s, new Metrics()) {
            @Override
            protected Completable doSend(String transportMessage, OutgoingMessageMetaData<String, TestTransportInfo> outgoingMessageMetaData) {
                throw new RuntimeException("for test");
            }
        };


        TestObserver testObserver = sut.sendMessage("origin", "message", null).test();
        testObserver.assertError((Predicate<Throwable>) t -> t instanceof RuntimeException && t.getMessage().equals("for test"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void marshallingErrorTransportedToCompletable() {
        this.sut = new MessageSender<String, String, String, TestTransportInfo>(
                s -> { throw new RuntimeException("for test"); }, new Metrics() ) {
            @Override
            protected Completable doSend(String transportMessage, OutgoingMessageMetaData<String, TestTransportInfo> outgoingMessageMetaData) {
                return Completable.complete();
            }
        };


        TestObserver testObserver = sut.sendMessage("origin", "message", null).test();
        testObserver.assertError((Predicate<Throwable>) t -> t instanceof RuntimeException && t.getMessage().equals("for test"));
    }

}
