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

import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;
import io.reactivex.subscribers.TestSubscriber;
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
    private RpcServer<String> sut;
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
        MessageReceiver<String> messageReceiver = new MessageReceiver<>(
                identifier,
                jmsObjectRepository,
                s -> s,
                metrics);
        mySender = new MySender(
                identifier,
                jmsObjectRepository,
                s -> s,
                metrics);

        sut = new RpcServer<>(identifier, messageReceiver, mySender, Throwable::getMessage, metrics);
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
        TestSubscriber<RpcInvocation<String>> testSubscriber = sut.requests(queue).test();

        jmsHelper.sendMessage(queue,"One");
        jmsHelper.sendRequest(queue,"Two");
        jmsHelper.sendMessage(queue,"Three");
        jmsHelper.sendRequest(queue,"Four");
        jmsHelper.sendMessage(queue,"Five");

        int maxLoops = 10;
        for (int i = 0; i < maxLoops && testSubscriber.valueCount() == 0; i++) {
            TimeUnit.MILLISECONDS.sleep(1000);
        }
        testSubscriber.assertValueCount(2);
    }

    @Test
    void completingRpcInvocationProperlyTriggersReplySending() throws Exception {
        Destination queue = jmsHelper.createQueue("completeRpc");
        TestSubscriber<RpcInvocation<String>> testSubscriber = sut.requests(queue).test();
        Message requestMessage = jmsHelper.sendRequest(queue, "Two");

        int maxLoops = 10;
        for (int i = 0; i < maxLoops && testSubscriber.valueCount()==0; i++) {
            TimeUnit.MILLISECONDS.sleep(1000);
        }
        testSubscriber.values().iterator().next().complete("reply");

        assertThat(mySender.message, is("reply"));
        assertNotNull(mySender.sendingInfo);
        assertThat(mySender.sendingInfo.destination, sameInstance(requestMessage.getJMSReplyTo()));
        assertThat(mySender.sendingInfo.details.correlationId, sameInstance(requestMessage.getJMSCorrelationID()));
        assertNull(mySender.sendingInfo.details.replyDestination);
        assertNotNull(mySender.sendingInfo.details.correlationId);
    }

    @Test
    void completingRpcInvocationExceptionallyTriggersReplySending() throws Exception {
        Destination queue = jmsHelper.createQueue("completeRpcExceptionally");
        TestSubscriber<RpcInvocation<String>> testSubscriber = sut.requests(queue).test();
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


    private class MySender extends MessageSender<String> {
        private String message;
        private OutgoingMessageMetaData sendingInfo;

        MySender(String identifier, JmsObjectRepository jmsObjectRepository, MessageMarshaller<String, String> messageMarshaller, Metrics metrics) {
            super(identifier, jmsObjectRepository, messageMarshaller, metrics);
        }


        @Override
        public Completable doSend(String message, OutgoingMessageMetaData meta) {
            this.message = message;
            this.sendingInfo = meta;
            return super.doSend(message, meta);
        }
    }
}