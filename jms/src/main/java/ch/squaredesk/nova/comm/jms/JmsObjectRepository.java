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

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class JmsObjectRepository {
    private final Logger logger = LoggerFactory.getLogger(JmsObjectRepository.class);

    private final Map<String, MessageConsumer> mapDestinationIdToMessageConsumer = new ConcurrentHashMap<>();
    private final Map<String, MessageProducer> mapDestinationIdToMessageProducer = new ConcurrentHashMap<>();

    private Session producerSession;
    private Session consumerSession;
    private Destination tempQueue;

    private final Connection connection;
    private final JmsSessionDescriptor producerSessionDescriptor;
    private final JmsSessionDescriptor consumerSessionDescriptor;
    private final Function<Destination, String> destinationIdGenerator;

    JmsObjectRepository(Connection connection,
                        JmsSessionDescriptor producerSessionDescriptor,
                        JmsSessionDescriptor consumerSessionDescriptor,
                        Function<Destination, String> destinationIdGenerator) {
        this.connection = connection;
        this.producerSessionDescriptor = producerSessionDescriptor;
        this.consumerSessionDescriptor = consumerSessionDescriptor;
        this.destinationIdGenerator = destinationIdGenerator;
    }

    Destination getPrivateTempQueue() {
        if (tempQueue == null) {
            try {
                tempQueue = consumerSession.createTemporaryQueue();
            } catch (JMSException e) {
                throw new RuntimeException("Unable to create temp queue",e);
            }
        }
        return tempQueue;
    }

    public Flowable<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>> messages(Destination destination, BackpressureStrategy backpressureStrategy) {
        return null;
    }

    MessageConsumer createMessageConsumer(Destination destination) throws JMSException {
        String destinationId = destinationIdGenerator.apply(destination);

        MessageConsumer consumer = mapDestinationIdToMessageConsumer.get(destinationId);
        if (consumer == null) {
            consumer = consumerSession.createConsumer(destination);
            mapDestinationIdToMessageConsumer.put(destinationId, consumer);

        }

        return consumer;
    }


    MessageProducer createMessageProducer(Destination destination) throws JMSException {
        String destinationId = destinationIdGenerator.apply(destination);
        MessageProducer producer = mapDestinationIdToMessageProducer.get(destinationId);
        if (producer == null) {
            producer = producerSession.createProducer(destination);
            mapDestinationIdToMessageProducer.put(destinationId, producer);
        }
        return producer;
    }

    TextMessage createTextMessage() throws JMSException {
        return producerSession.createTextMessage();
    }


    void destroyMessageProducer(Destination destination) throws JMSException {
        String destinationId = destinationIdGenerator.apply(destination);
        MessageProducer producer = mapDestinationIdToMessageProducer.remove(destinationId);
        if (producer != null) {
            try {
                producer.close();
            } catch (Exception e) {
                logger.warn("Unable to close producer for destination " + destinationId, e);
            }
        }
    }


    void destroyMessageConsumer(Destination destination) throws JMSException {
        String destinationId = destinationIdGenerator.apply(destination);
        MessageConsumer consumer = mapDestinationIdToMessageConsumer.remove(destinationId);
        if (consumer != null) {
            try {
                consumer.close();
            } catch (Exception e) {
                logger.warn("Unable to close producer for destination " + destinationId, e);
            }
        }
    }

    void start() throws JMSException {
        connection.start();
        logger.debug("Creating producer session with the following settings: {}",producerSessionDescriptor);
        this.producerSession = connection.createSession(producerSessionDescriptor.transacted, producerSessionDescriptor.acknowledgeMode);
        logger.debug("Creating consumer session with the following settings: {}",consumerSessionDescriptor);
        this.consumerSession = connection.createSession(consumerSessionDescriptor.transacted, consumerSessionDescriptor.acknowledgeMode);
    }

    void shutdown() {
        mapDestinationIdToMessageProducer.entrySet().forEach(entry -> {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.warn("Unable to close producer for destination " + entry.getKey(), e);
            }
        });
        mapDestinationIdToMessageProducer.clear();
        mapDestinationIdToMessageConsumer.entrySet().forEach(entry -> {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.warn("Unable to close consumer for destination " + entry.getKey(), e);
            }
        });
        mapDestinationIdToMessageConsumer.clear();
        try {
            consumerSession.close();
        } catch (Exception e) {
            logger.warn("Unable to close producer session", e);
        }
        try {
            producerSession.close();
        } catch (Exception e) {
            logger.warn("Unable to close producer session",e);
        }
        try {
            connection.close();
        } catch (Exception e) {
            logger.warn("Unable to close connection",e);
        }

    }

    String getIdForDestination (Destination destination) {
        return destinationIdGenerator.apply(destination);
    }

}
