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

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Tag("large")
class JmsIncomingMessageMetaDataCreatorTest {
    private TestJmsHelper jmsHelper;
    private JmsMessageMetaDataCreator sut;

    private EmbeddedActiveMQBroker broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");
        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();
        sut = new JmsMessageMetaDataCreator();
    }

    @AfterEach
    void tearDown() {
        broker.stop();
    }

    @Test
    void testCreateMessageDetails() throws JMSException {
        Destination incomingDestination = jmsHelper.createQueue("incoming");
        Message message = jmsHelper.createMessage("payload");
        message.setJMSDestination(incomingDestination);

        IncomingMessageMetaData<Destination, RetrieveInfo> details = sut.createIncomingMessageMetaData(message);
        assertThat(details.origin, is(incomingDestination));
    }
}
