/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
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
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("medium")
class MessageReceiverTest {
    private EmbeddedActiveMQBroker broker;
    private TestJmsHelper jmsHelper;
    private JmsObjectRepository objectRepository;
    private MessageReceiver sut;

    @BeforeEach
    void setup() throws Exception {
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

        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();

        sut = new MessageReceiver("MessageReceiverTest", objectRepository, new Metrics());
    }

    @AfterEach
    void tearDown() throws Exception {
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
    void messagesCanBeRetrievedFromQueue() throws Exception {
        CountDownLatch cdl = new CountDownLatch(5);
        List<String> receivedMessages = new ArrayList<>();
        Destination destination = jmsHelper.createQueue("1");
        sut.messages(destination)
                .map(im -> im.message)
                .subscribe(msg -> {
                    receivedMessages.add(msg);
                    cdl.countDown();
                });

        for (String msg: Arrays.asList("One", "Two", "3", "For", "Fve")) {
            jmsHelper.sendMessage(destination, msg);
        }
        cdl.await(5, TimeUnit.SECONDS);

        assertThat(cdl.getCount(), is(0L));
        assertThat(receivedMessages, contains("One", "Two", "3", "For", "Fve"));
    }

    @Test
    void messagesCanBeRetrievedByMultipleSubscribers() throws Exception {
        int numSubscribers = 5;
        CountDownLatch cdl = new CountDownLatch(5 * numSubscribers);
        List<String>[] receivedMessages = new ArrayList[numSubscribers];
        Destination destination = jmsHelper.createQueue("2");
        for (int i = 0; i < numSubscribers; i++) {
            int idx = i;
            receivedMessages[i] = new ArrayList<>();
            sut.messages(destination)
                    .map(im -> im.message)
                    .subscribe(msg -> {
                        receivedMessages[idx].add(msg);
                        cdl.countDown();
                    });
        }

        for (String msg: Arrays.asList("One", "Two", "3", "For", "Fve")) {
            jmsHelper.sendMessage(destination, msg);
        }
        cdl.await(5, TimeUnit.SECONDS);

        assertThat(cdl.getCount(), is(0L));
        for (int i = 0; i < numSubscribers; i++) {
            assertThat(receivedMessages[i], contains("One", "Two", "3", "For", "Fve"));
        }
    }

    @Test
    void subscribeWithNullDestinationThrows() throws Exception {
        assertThrows(NullPointerException.class, () -> sut.messages(null));
    }

}