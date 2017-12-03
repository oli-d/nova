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
import ch.squaredesk.nova.comm.retrieving.IncomingMessageDetails;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class JmsObjectRepository<InternalMessageType> {
    private final Logger logger = LoggerFactory.getLogger(JmsObjectRepository.class);

    private final Map<String, Flowable<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>>>
            mapDestinationIdToMessageConsumer = new ConcurrentHashMap<>();
    private final Map<String, MessageProducer> mapDestinationIdToMessageProducer = new ConcurrentHashMap<>();
    private final JmsMessageDetailsCreator messageDetailsCreator = new JmsMessageDetailsCreator();

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
                throw new RuntimeException("Unable to create temp queue", e);
            }
        }
        return tempQueue;
    }

    public Flowable<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>> messages(
            Destination destination, MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller) throws JMSException {

        String destinationId = destinationIdGenerator.apply(destination);
        // FIXME: cast
        return mapDestinationIdToMessageConsumer.computeIfAbsent(destinationId, key -> {
            Flowable<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>> f = Flowable.generate(
                    () -> {
                        logger.info("Opening connection to destination " + destinationId);
                        return createMessageConsumer(destination);
                    },
                    (consumer, emitter) -> {
                        IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo> incomingMessage = null;
                        while (incomingMessage == null) {
                            Message m = consumer.receive(); // FIXME: how do we signal that we do not want to receive any longer???
                            if (m == null) {
                                // we assume the consumer is closed
                                emitter.onComplete();
                                return;
                            }

                            if (!(m instanceof TextMessage)) {
                                logger.error("Unsupported type of incoming message " + m);
                                // TODO: metrics
                                continue;
                            }

                            String transportMessage;
                            try {
                                transportMessage = ((TextMessage) m).getText();
                            } catch (Exception e) {
                                logger.error("Unable to read incoming message " + m, e);
                                // TODO: metrics
                                continue;
                            }

                            InternalMessageType internalMessage = null;
                            try {
                                internalMessage = messageUnmarshaller.unmarshal(transportMessage);
                            } catch (Exception e) {
                                logger.error("Unable to unmarshal incoming message " + transportMessage, e);
                                // TODO: metrics
                                continue;
                            }

                            IncomingMessageDetails<Destination, JmsSpecificInfo> messageDetails =
                                    messageDetailsCreator.createMessageDetailsFor(m);
                            incomingMessage = new IncomingMessage<>(internalMessage, messageDetails);
                        }
                        if (incomingMessage != null)
                            emitter.onNext(incomingMessage);
                    },
                    consumer -> {
                        mapDestinationIdToMessageConsumer.remove(destinationId);
                        consumer.close();
                        logger.info("Closed connection to destination " + destinationId);
                    }
            );
            return f.subscribeOn(Schedulers.io())
                    .share();
        });
    }

    MessageConsumer createMessageConsumer(Destination destination) throws JMSException {
        MessageConsumer consumer = consumerSession.createConsumer(destination);
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
        // FIXME: implememnt
        /*
        String destinationId = destinationIdGenerator.apply(destination);
        MessageConsumer consumer = mapDestinationIdToMessageConsumer.remove(destinationId);
        if (consumer != null) {
            try {
                consumer.close();
            } catch (Exception e) {
                logger.warn("Unable to close producer for destination " + destinationId, e);
            }
        }
        */
    }

    void start() throws JMSException {
        connection.start();
        logger.debug("Creating producer session with the following settings: {}", producerSessionDescriptor);
        this.producerSession = connection.createSession(producerSessionDescriptor.transacted, producerSessionDescriptor.acknowledgeMode);
        logger.debug("Creating consumer session with the following settings: {}", consumerSessionDescriptor);
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
        // FIXME: implememnt
        /*
        mapDestinationIdToMessageConsumer.entrySet().forEach(entry -> {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.warn("Unable to close consumer for destination " + entry.getKey(), e);
            }
        });
        */
        mapDestinationIdToMessageConsumer.clear();
        try {
            consumerSession.close();
        } catch (Exception e) {
            logger.warn("Unable to close producer session", e);
        }
        try {
            producerSession.close();
        } catch (Exception e) {
            logger.warn("Unable to close producer session", e);
        }
        try {
            connection.close();
        } catch (Exception e) {
            logger.warn("Unable to close connection", e);
        }

    }

}
