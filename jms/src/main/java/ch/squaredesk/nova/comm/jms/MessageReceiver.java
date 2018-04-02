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
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

class MessageReceiver<InternalMessageType>
        extends ch.squaredesk.nova.comm.retrieving.MessageReceiver<Destination, InternalMessageType, String, IncomingMessageMetaData> {

    private static final Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

    private final JmsObjectRepository jmsObjectRepository;

    private final Map<String, Flowable<IncomingMessage<InternalMessageType, IncomingMessageMetaData>>>
            mapDestinationIdToMessageStream = new ConcurrentHashMap<>();
    private final JmsMessageMetaDataCreator messageDetailsCreator = new JmsMessageMetaDataCreator();

    MessageReceiver(String identifier,
                              JmsObjectRepository jmsObjectRepository,
                              MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                              Metrics metrics) {
        super(identifier, messageUnmarshaller, metrics);
        this.jmsObjectRepository = jmsObjectRepository;
    }

    @Override
    public Flowable<IncomingMessage<InternalMessageType, IncomingMessageMetaData>> messages(Destination destination) {
        Objects.requireNonNull(destination, "origin must not ne bull");

        String destinationId = jmsObjectRepository.idFor(destination);
        return mapDestinationIdToMessageStream.computeIfAbsent(destinationId, key -> {
            Flowable<IncomingMessage<InternalMessageType, IncomingMessageMetaData>> f = Flowable.generate(
                    () -> {
                        logger.info("Opening connection to origin " + destinationId);
                        metricsCollector.subscriptionCreated(destinationId);
                        return jmsObjectRepository.createMessageConsumer(destination);
                    },
                    (consumer, emitter) -> {
                        IncomingMessage<InternalMessageType, IncomingMessageMetaData> incomingMessage = null;
                        while (incomingMessage == null) {
                            Message m = null;
                            try {
                                m = consumer.receive();
                            } catch (Exception e) {
                                // noop, the consumer was closed
                            }

                            if (m == null) {
                                logger.info("Unable to receive message from consumer for origin " + destinationId + ". Closing the connection...");
                                emitter.onComplete();
                                return;
                            }

                            if (!(m instanceof TextMessage)) {
                                logger.error("Unsupported type of incoming message " + m);
                                metricsCollector.unparsableMessageReceived(destinationId);
                                continue;
                            }

                            String transportMessage;
                            try {
                                transportMessage = ((TextMessage) m).getText();
                            } catch (Exception e) {
                                logger.error("Unable to read incoming message " + m, e);
                                metricsCollector.unparsableMessageReceived(destinationId);
                                continue;
                            }

                            InternalMessageType internalMessage;
                            try {
                                internalMessage = messageUnmarshaller.unmarshal(transportMessage);
                            } catch (Exception e) {
                                logger.error("Unable to unmarshal incoming message " + transportMessage, e);
                                metricsCollector.unparsableMessageReceived(destinationId);
                                continue;
                            }

                            IncomingMessageMetaData meta = messageDetailsCreator.createIncomingMessageMetaData(m);
                            incomingMessage = new IncomingMessage<>(internalMessage, meta);
                            metricsCollector.messageReceived(destinationId);
                        }
                        emitter.onNext(incomingMessage);
                    },
                    consumer -> {
                        metricsCollector.subscriptionDestroyed(destinationId);
                        jmsObjectRepository.destroyConsumer(consumer);
                        mapDestinationIdToMessageStream.remove(destinationId);
                        logger.info("Closed connection to origin " + destinationId);
                    }
            );
            return f.subscribeOn(Schedulers.io())
                    .share();
        });
    }

}
