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

import javax.jms.*;
import java.util.UUID;

public class TestJmsHelper {
    private final ConnectionFactory connectionFactory;
    private MessageProducer replyProducer = null;
    private Connection connection;
    private Session session;

    public TestJmsHelper(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public TextMessage createMessage (String payload) throws JMSException {
        return session.createTextMessage(payload);
    }

    public TextMessage createRequest (String payload) throws JMSException {
        Destination replyQ = createTempQueue();
        TextMessage message = session.createTextMessage(payload);
        message.setJMSCorrelationID(UUID.randomUUID().toString());
        message.setJMSReplyTo(replyQ);
        return message;
    }

    public Message sendMessage(Destination destination, String msg) throws JMSException {
        MessageProducer producer = session.createProducer(destination);
        Message message = createMessage(msg);
        producer.send(message);
        producer.close();
        return message;
    }

    public Message sendRequest(Destination destination, String request) throws JMSException {
        MessageProducer producer = session.createProducer(destination);
        Message message = createRequest(request);
        producer.send(message);
        producer.close();
        return message;
    }

    public Destination echoOnQueue (String destination) throws JMSException {
        Destination queue = createQueue(destination);
        MessageConsumer consumer = session.createConsumer(queue);
        consumer.setMessageListener(message -> {
            try {
                Destination replyTo = message.getJMSReplyTo();
                if (replyProducer==null) {
                    replyProducer = session.createProducer(replyTo);
                }
                Message replyMessage = session.createTextMessage(((TextMessage)message).getText());
                replyMessage.setJMSCorrelationID(message.getJMSCorrelationID());
                replyProducer.send(replyTo,replyMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return queue;
    }

    public Destination createQueue (String destination) throws JMSException {
        return session.createQueue(destination);
    }

    public MessageConsumer createMessageConsumer (Destination destination) throws JMSException {
        return session.createConsumer(destination);
    }

    public void start() throws JMSException {
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
    }

    public void shutdown() throws JMSException {
        connection.close();
    }

    public TemporaryQueue createTempQueue() throws JMSException {
        return session.createTemporaryQueue();
    }

}
