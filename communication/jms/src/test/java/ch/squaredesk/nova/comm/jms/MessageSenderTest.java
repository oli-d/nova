/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData;
import io.reactivex.rxjava3.observers.TestObserver;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jms.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@Tag("medium")
class MessageSenderTest {
    private EmbeddedActiveMQBroker broker;
    private TestJmsHelper jmsHelper;
    private MessageSender sut;

    @BeforeEach
    void setup() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();
        if (!broker.brokerService.waitUntilStarted()) throw new RuntimeException("Unable to start embedded broker...");

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");

        JmsObjectRepository objectRepository = new JmsObjectRepository(
                connectionFactory.createConnection(),
                new JmsSessionDescriptor(false, Session.AUTO_ACKNOWLEDGE),
                new JmsSessionDescriptor(false, Session.AUTO_ACKNOWLEDGE),
                String::valueOf
        );
        objectRepository.start();

        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();

        sut = new MessageSender("MessageSenderTest", objectRepository);
    }

    @AfterEach
    void tearDown() {
        try {
            jmsHelper.shutdown();
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
    void sendNullMessageReturnsError() throws Exception {
        setup();
        OutgoingMessageMetaData<Destination, SendInfo> meta = new OutgoingMessageMetaData<>(
                jmsHelper.createQueue("not used"), null);
        assertThrows(NullPointerException.class, () -> sut.send(null, meta));
    }

    @Test
    void sendMessage() throws Exception {
        setup();
        Destination queue = jmsHelper.createQueue("sendTest");
        OutgoingMessageMetaData<Destination, SendInfo> meta = new OutgoingMessageMetaData<>(queue, null);
        MessageConsumer consumer = jmsHelper.createMessageConsumer(queue);

        TestObserver<OutgoingMessageMetaData<Destination, SendInfo>> observer = sut.send("Hallo", meta).test();
        observer.await(10, SECONDS);
        observer.assertComplete();

        Message message = consumer.receive(1000L);
        assertNotNull(message);
        assertTrue(message instanceof TextMessage);
        MatcherAssert.assertThat(((TextMessage) message).getText(), is("Hallo"));
    }

    @Test
    void sendMessageWithException() throws Exception {
        Destination queue = jmsHelper.createQueue("sendTest");

        OutgoingMessageMetaData<Destination, SendInfo> meta = new OutgoingMessageMetaData<>(queue, null);
        TestObserver<OutgoingMessageMetaData<Destination, SendInfo>> observer = sut.send("Hallo", meta, message -> {
            throw new RuntimeException("4 test");
        }).test();
        observer.await(1, SECONDS);
        observer.assertError(t-> t instanceof RuntimeException && t.getMessage().equals("4 test"));
    }
}