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
    private MessageSender<String, String, String, OutgoingMessageMetaData<String, TestTransportInfo>> sut;
    private List<String> transportMessages;
    private List<OutgoingMessageMetaData<String, TestTransportInfo>> outgoingMessageMetaData;

    @BeforeEach
    void setUp() {
        this.sut = new MessageSender<String, String, String, OutgoingMessageMetaData<String, TestTransportInfo>>(s -> s, new Metrics()) {
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
                () -> new MessageSender<Object,Object, Object, OutgoingMessageMetaData<Object, Object>>(null, new Metrics()) {
                    @Override
                    protected Completable doSend(Object transportMessage, OutgoingMessageMetaData messageSendingInfo) {
                        return null;
                    }
                });
        assertThat(t.getMessage(), containsString("messageMarshaller"));
    }

}
