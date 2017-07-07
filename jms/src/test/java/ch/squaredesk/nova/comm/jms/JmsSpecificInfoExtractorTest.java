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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class JmsSpecificInfoExtractorTest {
    private TestJmsHelper jmsHelper;
    private EmbeddedActiveMQBroker broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");
        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();
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

        JmsSpecificInfo info = JmsSpecificInfoExtractor.extractFrom(message);
        assertThat(info.correlationId, is("c1"));
        assertThat(info.customHeaders.size(), is(2));
        assertThat(info.customHeaders.get("k1"), is("v1"));
        assertThat(info.customHeaders.get("k3"), is("v2"));
        assertThat(info.replyDestination, is(replyDestination));
    }
}
