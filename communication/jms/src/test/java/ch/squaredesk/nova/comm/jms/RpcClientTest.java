/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.rpc.RpcReply;
import ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData;
import io.reactivex.rxjava3.observers.TestObserver;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Session;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Tag("medium")
class RpcClientTest {
    private RpcClient sut;
    private TestJmsHelper jmsHelper;
    private JmsObjectRepository objectRepository;

    private EmbeddedActiveMQBroker broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();
        if (!broker.brokerService.waitUntilStarted()) throw new RuntimeException("Unable to start embedded broker...");

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");

        objectRepository = new JmsObjectRepository(
                connectionFactory.createConnection(),
                new JmsSessionDescriptor(false, Session.AUTO_ACKNOWLEDGE),
                new JmsSessionDescriptor(false, Session.AUTO_ACKNOWLEDGE),
                String::valueOf
        );
        objectRepository.start();

        MessageReceiver messageReceiver = new MessageReceiver("RpcClientTest", objectRepository);
        MessageSender messageSender = new MessageSender("RpcClientTest", objectRepository);
        sut = new RpcClient("id", messageSender, messageReceiver);

        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();
    }

    @AfterEach
    void tearDown() {
        try {
            jmsHelper.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            objectRepository.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            broker.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void noReplyWithinTimeoutReturnsTimeoutException() throws Exception {
        Destination queue = jmsHelper.createQueue("request1");
        Destination replyTo = jmsHelper.createQueue("replyTo1");
        SendInfo sendingDetails = new SendInfo("c1",
                replyTo,
                Message.DEFAULT_DELIVERY_MODE,
                Message.DEFAULT_PRIORITY,
                Message.DEFAULT_TIME_TO_LIVE);
        OutgoingMessageMetaData<Destination, SendInfo> metaData = new OutgoingMessageMetaData<>(queue, sendingDetails);

        TestObserver<RpcReply<String, IncomingMessageMetaData<Destination, RetrieveInfo>>> replyObserver =
                sut.sendRequest("aRequest", metaData, s -> s, s -> s, Duration.ofMillis(250)).test().await();

        replyObserver.assertError(TimeoutException.class);
    }

    @Test
    void replyProperlyDispatchedToCaller() throws Exception {
        Destination queue = jmsHelper.createQueue("request2");
        Destination replyTo = jmsHelper.createQueue("replyTo2");
        SendInfo sendingDetails = new SendInfo("c2",
                replyTo,
                Message.DEFAULT_DELIVERY_MODE,
                Message.DEFAULT_PRIORITY,
                Message.DEFAULT_TIME_TO_LIVE);
        OutgoingMessageMetaData<Destination, SendInfo> metaData = new OutgoingMessageMetaData<>(queue, sendingDetails);

        TestObserver<RpcReply<String, IncomingMessageMetaData<Destination, RetrieveInfo>>> replyObserver =
                sut.sendRequest("aRequest", metaData, s -> s, s -> s, Duration.ofSeconds(20)).test();
        jmsHelper.sendReply(replyTo, "aReply", "c2");

        replyObserver.await(21, SECONDS);
        replyObserver.assertComplete();
        replyObserver.assertValueCount(1);
        assertThat(replyObserver.values().get(0).result(), is("aReply"));
    }

    @Test
    void subscriptionAndRpcRepliesSupportedOnSameQueue() throws Exception {
        Destination queue = jmsHelper.createQueue("request3");
        Destination sharedQueue = jmsHelper.createQueue("someDest");
        SendInfo sendingDetails = new SendInfo("c3",
                sharedQueue,
                Message.DEFAULT_DELIVERY_MODE,
                Message.DEFAULT_PRIORITY,
                Message.DEFAULT_TIME_TO_LIVE);
        OutgoingMessageMetaData<Destination, SendInfo> metaData = new OutgoingMessageMetaData<>(queue, sendingDetails);

        TestObserver<RpcReply<String, IncomingMessageMetaData<Destination, RetrieveInfo>>> replyObserver =
                sut.sendRequest("aRequest", metaData, s -> s, s -> s, Duration.ofSeconds(20)).test();
        jmsHelper.sendReply(sharedQueue, "aReply1", null);
        jmsHelper.sendReply(sharedQueue, "aReply2", "c3");
        jmsHelper.sendReply(sharedQueue, "aReply3", null);

        replyObserver.await(21, SECONDS);
        replyObserver.assertComplete();
        replyObserver.assertValueCount(1);
        assertThat(replyObserver.values().get(0).result(), is("aReply2"));
    }
}
