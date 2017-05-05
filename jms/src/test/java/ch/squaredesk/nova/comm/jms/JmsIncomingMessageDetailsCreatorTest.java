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

import ch.squaredesk.nova.comm.retrieving.IncomingMessageDetails;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class JmsIncomingMessageDetailsCreatorTest {
    private TestJmsHelper jmsHelper;
    private JmsMessageDetailsCreator sut;

    private EmbeddedActiveMQBroker broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");
        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();
        sut = new JmsMessageDetailsCreator();
    }

    @AfterEach
    void tearDown() {
        broker.stop();
    }

    @Test
    void testCreateMessageDetails() throws JMSException {
        Destination replyDestination = jmsHelper.createQueue("reply");
        Destination incomingDestination = jmsHelper.createQueue("incoming");
        Message message = jmsHelper.createMessage("payload");
        message.setJMSDestination(incomingDestination);
        message.setJMSCorrelationID("c1");
        message.setJMSReplyTo(replyDestination);
        message.setObjectProperty("k1", "v1");
        message.setObjectProperty("k3", "v2");

        IncomingMessageDetails<Destination, JmsSpecificInfo> details = sut.createMessageDetailsFor(message);
        assertThat(details.transportSpecificDetails.correlationId, is("c1"));
        assertThat(details.transportSpecificDetails.customHeaders.size(), is(2));
        assertThat(details.transportSpecificDetails.customHeaders.get("k1"), is("v1"));
        assertThat(details.transportSpecificDetails.customHeaders.get("k3"), is("v2"));
        assertThat(details.destination, is(incomingDestination));
        assertThat(details.transportSpecificDetails.replyDestination, is(replyDestination));
    }
}
