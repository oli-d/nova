/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jms.Destination;
import javax.jms.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JmsAdapterTest {
    private static final int defaultDeliveryMode = 1;
    private static final int defaultPriority = 2;
    private static final long defaultTtl = 3;
    private static final long defaultRpcTimeout = 4;
    private static final TimeUnit defaultRpcTimeoutUnit = MINUTES;

    private MyMessageReceiver myMessageReceiver;
    private MyMessageSender myMessageSender;
    private MyRpcClient myRpcClient;
    private JmsAdapter sut;

    @BeforeEach
    void setUp() {
        myMessageReceiver = new MyMessageReceiver();
        myMessageSender = new MyMessageSender();
        myRpcClient = new MyRpcClient();

        sut = JmsAdapter.builder()
                .setJmsObjectRepository(new MyJmsObjectRepository())
                .setMessageReceiver(myMessageReceiver)
                .setMessageSender(myMessageSender)
                .setRpcClient(myRpcClient)
                .setRpcServer(null)
                .setDefaultMessageDeliveryMode(defaultDeliveryMode)
                .setDefaultMessageTimeToLive(defaultTtl)
                .setDefaultMessagePriority(defaultPriority)
                .setDefaultRpcTimeout(defaultRpcTimeout, defaultRpcTimeoutUnit)
                .build();
    }

    @Test
    void instanceCannotBeCreatedWithoutConnectionFactory() {
        Throwable t = assertThrows(NullPointerException.class,
                () -> JmsAdapter.builder()
                .setRpcClient(null)
                .build());
        assertThat(t.getMessage(),containsString("connectionFactory"));
    }

    @Test
    void nullDestinationThrows() {
        assertThrows(NullPointerException.class,
                ()-> sut.sendRequest(null, "some request", String.class));
    }

    @Test
    void customHeadersGetTransported() {
        Map<String, Object> sendHeaders = new HashMap<>();
        sendHeaders.put("k1","v1");
        sendHeaders.put("k2","v2");

        MyDestination destination = new MyDestination();
        sut.sendMessage(destination, "some message", sendHeaders);

        assertThat(myMessageSender.message, is("some message"));
        assertThat(myMessageSender.metaData.destination, is(destination));
        assertThat(myMessageSender.metaData.details.customHeaders, is(sendHeaders));
    }

    @Test
    void defaultJmsDeliveryPropertiesAreUsedIfNotSpecified() {
        MyDestination destination = new MyDestination();

        sut.sendMessage(destination, "some message");

        assertThat(myMessageSender.metaData.details.deliveryMode, is(defaultDeliveryMode));
        assertThat(myMessageSender.metaData.details.priority, is(defaultPriority));
        assertThat(myMessageSender.metaData.details.timeToLive, is(defaultTtl));
    }

    @Test
    void messagesStreamOnlyDeliversNonRpcMessages() {
        MyDestination origin = new MyDestination();
        RetrieveInfo rpcInfo = new RetrieveInfo("c1", null, null);
        RetrieveInfo messageInfo = new RetrieveInfo(null, null, null);
        IncomingMessage[] messages = new IncomingMessage[] {
                new IncomingMessage<>("one", new IncomingMessageMetaData(origin, null, rpcInfo)),
                new IncomingMessage<>("two", new IncomingMessageMetaData(origin, null, messageInfo)),
                new IncomingMessage<>("three", new IncomingMessageMetaData(origin, null, messageInfo)),
                new IncomingMessage<>("four", new IncomingMessageMetaData(origin, null, messageInfo)),
                new IncomingMessage<>("five", new IncomingMessageMetaData(origin, null, rpcInfo))
        };

        TestSubscriber<String> subscriber = sut.messages(origin).test();
        for (IncomingMessage msg: messages) {
            myMessageReceiver.publishSubject.onNext(msg);
        }

        assertThat(subscriber.valueCount(), is(3));
        assertThat(subscriber.values().get(0), is(messages[1].message));
        assertThat(subscriber.values().get(1), is(messages[2].message));
        assertThat(subscriber.values().get(2), is(messages[3].message));
    }

    @Test
    void subscribingToRequestsCannotBeDoneWithNullDestination() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> sut.requests(null, String.class));
        assertThat(npe.getMessage(), containsString("destination must not be null"));
    }

    @Test
    void jmsReplyToAddedToRequestSendingInfo() {
        sut.sendRequest(new MyDestination(), "message", String.class);

        assertNotNull(myRpcClient.outgoingMessageMetaData.details.replyDestination);
    }

    @Test
    void jmsReplyToCanBeSpecified() {
        MyDestination destination = new MyDestination();
        MyDestination replyToDestination = new MyDestination();
        sut.sendRequest(destination, replyToDestination, "message", null, String.class, 1, SECONDS);

        assertThat(myRpcClient.outgoingMessageMetaData.details.replyDestination, is(replyToDestination));
    }

    @Test
    void destinationMustBeProvidedOnRpc() {
        assertThrows(NullPointerException.class,
                () -> sut.sendRequest(null, "message", null, String.class, 100L, MILLISECONDS));
    }

    @Test
    void timeUnitMustBeProvidedIfTimeoutIsNotNull() {
        assertThrows(NullPointerException.class,
                () -> sut.sendRequest(new MyDestination(), "message", String.class, 1777L, null));
    }

    @Test
    void defaultTimeoutUsedForRpcIfNoTimeoutProvided()  {
        sut.sendRequest(new MyDestination(), "message", String.class);

        assertThat(myRpcClient.timeout, is(defaultRpcTimeout));
        assertThat(myRpcClient.timeUnit, is(defaultRpcTimeoutUnit));
    }

    @Test
    void correlationIdGeneratedForRpcs() throws Exception {
        sut.sendRequest(new MyDestination(), "message", String.class);

        assertNotNull(myRpcClient.outgoingMessageMetaData.details.correlationId);
    }

    @Test
    void destinationAndMessagePassedUnchangedForSend() {
        MyDestination destination = new MyDestination();

        sut.sendMessage(destination, "some message");

        assertThat(myMessageSender.message, is("some message"));
        assertThat(myMessageSender.metaData.destination, is(destination));
    }

    @Test
    void destinationAndMessagePassedUnchangedForRpc() {
        MyDestination destination = new MyDestination();

        sut.sendRequest(destination, "some message", String.class);

        assertThat(myRpcClient.request, is("some message"));
        assertThat(myRpcClient.outgoingMessageMetaData.destination, is(destination));
    }

    private class MyMessageReceiver extends MessageReceiver {
        private Subject<IncomingMessage<String, IncomingMessageMetaData>> publishSubject = PublishSubject.create();
        private Destination destination;

        MyMessageReceiver() {
            super("", null, new Metrics());
        }

        @Override
        public Flowable<IncomingMessage<String, IncomingMessageMetaData>> messages(Destination destination) {
            this.destination = destination;
            return publishSubject.toFlowable(BackpressureStrategy.BUFFER);
        }
    }

    private class MyMessageSender extends MessageSender {
        private String message;
        private OutgoingMessageMetaData metaData;

        MyMessageSender() {
            super("", null, new Metrics());
        }

        @Override
        public Single<OutgoingMessageMetaData> send(String message, OutgoingMessageMetaData meta) {
            this.message = message;
            this.metaData = meta;
            return Single.just(meta);
        }
    }

    private class MyRpcClient extends RpcClient {
        private Object request;
        private long timeout;
        private TimeUnit timeUnit;
        private OutgoingMessageMetaData outgoingMessageMetaData;

        private MyRpcClient() {
            super("TestRpcClient", null, null, new Metrics());
        }

        @Override
        public <RequestType, ReplyType> Single<RpcReply<ReplyType>> sendRequest(RequestType request, OutgoingMessageMetaData requestMetaData, Function<RequestType, String> requestTranscriber, Function<String, ReplyType> replyTranscriber, long timeout, TimeUnit timeUnit) {
            this.request = request;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.outgoingMessageMetaData = requestMetaData;
            return Single.never();
        }
    }

    private class MyDestination implements Destination {
    }

    private class MyJmsObjectRepository extends JmsObjectRepository {
        MyJmsObjectRepository() {
            super(null,
                    new JmsSessionDescriptor(false, Session.AUTO_ACKNOWLEDGE),
                    new JmsSessionDescriptor(false, Session.AUTO_ACKNOWLEDGE),
                    String::valueOf);
        }

        @Override
        Destination getPrivateTempQueue() {
            return new MyDestination();
        }
    }
}
