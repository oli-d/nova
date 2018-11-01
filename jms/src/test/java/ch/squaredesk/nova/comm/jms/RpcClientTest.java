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

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.observers.TestObserver;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Session;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
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

        Metrics metrics = new Metrics();
        MessageReceiver messageReceiver = new MessageReceiver("RpcClientTest",
                objectRepository,
                metrics);
        MessageSender messageSender = new MessageSender("RpcClientTest",
                objectRepository,
                metrics);
        sut = new RpcClient("id", messageSender, messageReceiver, metrics);

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
                null,
                Message.DEFAULT_DELIVERY_MODE,
                Message.DEFAULT_PRIORITY,
                Message.DEFAULT_TIME_TO_LIVE);
        OutgoingMessageMetaData metaData = new OutgoingMessageMetaData(queue, sendingDetails);

        TestObserver<RpcReply<String>> replyObserver = sut.sendRequest("aRequest", metaData, s -> s, s -> s, 250, MILLISECONDS).test().await();

        replyObserver.assertError(TimeoutException.class);
    }

    @Test
    void replyProperlyDispatchedToCaller() throws Exception {
        Destination queue = jmsHelper.createQueue("request2");
        Destination replyTo = jmsHelper.createQueue("replyTo2");
        SendInfo sendingDetails = new SendInfo("c2",
                replyTo,
                null,
                Message.DEFAULT_DELIVERY_MODE,
                Message.DEFAULT_PRIORITY,
                Message.DEFAULT_TIME_TO_LIVE);
        OutgoingMessageMetaData metaData = new OutgoingMessageMetaData(queue, sendingDetails);

        TestObserver<RpcReply<String>> replyObserver = sut.sendRequest("aRequest", metaData, s -> s, s -> s, 20, SECONDS).test();
        jmsHelper.sendReply(replyTo, "aReply", "c2");

        replyObserver.await(21, SECONDS);
        replyObserver.assertComplete();
        replyObserver.assertValueCount(1);
        assertThat(replyObserver.values().get(0).result, is("aReply"));
    }

    @Test
    void subscriptionAndRpcRepliesSupportedOnSameQueue() throws Exception {
        Destination queue = jmsHelper.createQueue("request3");
        Destination sharedQueue = jmsHelper.createQueue("someDest");
        SendInfo sendingDetails = new SendInfo("c3",
                sharedQueue,
                null,
                Message.DEFAULT_DELIVERY_MODE,
                Message.DEFAULT_PRIORITY,
                Message.DEFAULT_TIME_TO_LIVE);
        OutgoingMessageMetaData metaData = new OutgoingMessageMetaData(queue, sendingDetails);

        TestObserver<RpcReply<String>> replyObserver = sut.sendRequest("aRequest", metaData, s -> s, s -> s, 20, SECONDS).test();
        jmsHelper.sendReply(sharedQueue, "aReply1", null);
        jmsHelper.sendReply(sharedQueue, "aReply2", "c3");
        jmsHelper.sendReply(sharedQueue, "aReply3", null);

        replyObserver.await(21, SECONDS);
        replyObserver.assertComplete();
        replyObserver.assertValueCount(1);
        assertThat(replyObserver.values().get(0).result, is("aReply2"));
    }
}
