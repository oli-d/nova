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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.TemporaryQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

class CommAdapterDestinationListenerTest {
    private JmsAdapter<String> sut;
    private TestJmsHelper jmsHelper;

    private EmbeddedActiveMQBroker broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");
        sut = JmsAdapter.builder(String.class)
                .setConnectionFactory(connectionFactory)
                .setErrorReplyFactory(error -> "Error")
                .build();
        sut.start();

        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        sut.shutdown();
        jmsHelper.shutdown();
        broker.stop();
    }


    @Test
    void destinationListenerCalledWhenTempQueueIsDeleted() throws Exception {
        TemporaryQueue queue = jmsHelper.createTempQueue();
        queue.delete();

        Destination deadDestinationHolder[] = new Destination[1];
        sut.addDestinationListener(destination -> deadDestinationHolder[0] = destination);

        sut.sendMessage(queue, "Test").test().await(2, TimeUnit.SECONDS);
        assertThat(deadDestinationHolder[0], Matchers.sameInstance(queue));
    }

    @Test
    void testMessageSendingWithExceptionSignallingDestinationDownInExceptionChain() throws Exception {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");
        RuntimeException myException = new RuntimeException("Outer", new InvalidDestinationException("for test"));
        sut.shutdown();
        sut = JmsAdapter.builder(String.class)
                .setConnectionFactory(connectionFactory)
                .setErrorReplyFactory(error -> "Error")
                .setMessageMarshaller(message -> {
                    throw myException;
                })
                .build();
        sut.start();

        Destination queue = jmsHelper.createQueue("deadQueue");
        Destination deadDestinationHolder[] = new Destination[1];
        sut.addDestinationListener(destination -> deadDestinationHolder[0] = destination);

        sut.sendMessage(queue, "Test").test().await(2, TimeUnit.SECONDS);
        assertThat(deadDestinationHolder[0], Matchers.sameInstance(queue));
    }


}
