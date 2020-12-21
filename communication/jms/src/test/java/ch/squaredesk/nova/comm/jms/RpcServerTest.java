/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jms.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("large")
class RpcServerTest {
    private RpcServer sut;
    private TestJmsHelper jmsHelper;
    private MySender mySender;

    private EmbeddedActiveMQBroker broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();

        String identifier = "RpcServerTest";
        Metrics metrics = new Metrics();
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");
        Connection connection = connectionFactory.createConnection();
        JmsSessionDescriptor producerSessionDescriptor = new JmsSessionDescriptor(false, Session.AUTO_ACKNOWLEDGE);
        JmsSessionDescriptor consumerSessionDescriptor = new JmsSessionDescriptor(false, Session.AUTO_ACKNOWLEDGE);
        JmsObjectRepository jmsObjectRepository = new JmsObjectRepository(connection, producerSessionDescriptor, consumerSessionDescriptor, String::valueOf);
        MessageReceiver messageReceiver = new MessageReceiver(
                identifier,
                jmsObjectRepository,
                metrics);
        mySender = new MySender(
                identifier,
                jmsObjectRepository,
                metrics);

        sut = new RpcServer(identifier, messageReceiver, mySender, metrics);
        jmsObjectRepository.start();

        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();
    }

    @AfterEach
    void tearDown() {
        broker.stop();
    }

    @Test
    void subribeToIncomingRequests() throws Exception {
        Destination queue = jmsHelper.createQueue("subscribeToRequests");
        TestSubscriber<RpcInvocation<String>> testSubscriber = sut.requests(queue, String.class).test();

        jmsHelper.sendMessage(queue,"One");
        jmsHelper.sendRequest(queue,"Two");
        jmsHelper.sendMessage(queue,"Three");
        jmsHelper.sendRequest(queue,"Four");
        jmsHelper.sendMessage(queue,"Five");

        int maxLoops = 10;
        for (int i = 0; i < maxLoops && testSubscriber.values().size() == 0; i++) {
            TimeUnit.MILLISECONDS.sleep(1000);
        }
        testSubscriber.assertValueCount(2);
    }

    @Test
    void completingRpcInvocationProperlyTriggersReplySending() throws Throwable {
        Destination queue = jmsHelper.createQueue("completeRpc");
        TestSubscriber<RpcInvocation<String>> testSubscriber = sut.requests(queue, String.class).test();
        Message requestMessage = jmsHelper.sendRequest(queue, "Two");

        int maxLoops = 10;
        for (int i = 0; i < maxLoops && testSubscriber.values().size()==0; i++) {
            TimeUnit.MILLISECONDS.sleep(1000);
        }
        testSubscriber.values().iterator().next().complete("reply", s->s);

        assertThat(mySender.message, is("reply"));
        assertNotNull(mySender.sendingInfo);
        assertThat(mySender.sendingInfo.destination, sameInstance(requestMessage.getJMSReplyTo()));
        assertThat(mySender.sendingInfo.details.correlationId, sameInstance(requestMessage.getJMSCorrelationID()));
        assertNull(mySender.sendingInfo.details.replyDestination);
        assertNotNull(mySender.sendingInfo.details.correlationId);
    }

    /** TODO: this was valid when we were returning an error message for server side errors. Keeping it in here
     * since we're are not sure, whether it should be re-introduced
    @Test
    void completingRpcInvocationExceptionallyTriggersReplySending() throws Exception {
        Destination queue = jmsHelper.createQueue("completeRpcExceptionally");
        TestSubscriber<RpcInvocation<String>> testSubscriber = sut.requests(queue, String.class).test();
        Message requestMessage = jmsHelper.sendRequest(queue, "Boom");

        int maxLoops = 10;
        for (int i = 0; i < maxLoops && testSubscriber.valueCount()==0; i++) {
            TimeUnit.MILLISECONDS.sleep(1000);
        }
        testSubscriber.values().iterator().next().completeExceptionally(new RuntimeException("4test"));

        assertThat(mySender.message, is("4test"));
        assertNotNull(mySender.sendingInfo);
        assertThat(mySender.sendingInfo.destination, sameInstance(requestMessage.getJMSReplyTo()));
        assertThat(mySender.sendingInfo.details.correlationId, sameInstance(requestMessage.getJMSCorrelationID()));
        assertNull(mySender.sendingInfo.details.replyDestination);
        assertNotNull(mySender.sendingInfo.details.correlationId);
    }
    **/

    private class MySender extends MessageSender {
        private String message;
        private OutgoingMessageMetaData sendingInfo;

        MySender(String identifier, JmsObjectRepository jmsObjectRepository, Metrics metrics) {
            super(identifier, jmsObjectRepository, metrics);
        }


        @Override
        public Single<OutgoingMessageMetaData> send(String message, OutgoingMessageMetaData meta) {
            this.message = message;
            this.sendingInfo = meta;
            return super.send(message, meta);
        }
    }
}