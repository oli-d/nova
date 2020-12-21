/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

class MessageReceiver
        extends ch.squaredesk.nova.comm.retrieving.MessageReceiver<Destination, String, IncomingMessageMetaData> {

    private static final Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

    private final JmsObjectRepository jmsObjectRepository;

    private final Map<String, Flowable<IncomingMessage<String, IncomingMessageMetaData>>>
            mapDestinationIdToMessageStream = new ConcurrentHashMap<>();
    private final JmsMessageMetaDataCreator messageDetailsCreator = new JmsMessageMetaDataCreator();

    MessageReceiver(String identifier,
                    JmsObjectRepository jmsObjectRepository,
                    Metrics metrics) {
        super(Metrics.name("jms", identifier), metrics);
        this.jmsObjectRepository = jmsObjectRepository;
    }

    @Override
    public Flowable<IncomingMessage<String, IncomingMessageMetaData>> messages(Destination destination) {
        Objects.requireNonNull(destination, "destination must not ne bull");
        Objects.requireNonNull(destination, "unmarshaller must not ne bull");
        String destinationId = jmsObjectRepository.idFor(destination);
        return mapDestinationIdToMessageStream.computeIfAbsent(destinationId, key -> {
            Flowable<IncomingMessage<String, IncomingMessageMetaData>> f = Flowable.generate(
                    () -> {
                        logger.info("Opening connection to destination {}", destinationId);
                        metricsCollector.subscriptionCreated(destinationId);
                        return jmsObjectRepository.createMessageConsumer(destination);
                    },
                    (consumer, emitter) -> {
                        IncomingMessage<String, IncomingMessageMetaData> incomingMessage = null;
                        while (incomingMessage == null) {
                            Message m = null;
                            try {
                                metricsCollector.messageReceived(destinationId);
                                m = consumer.receive();
                            } catch (Exception e) {
                                // noop, the consumer was closed
                            }

                            if (m == null) {
                                logger.info("Unable to receive message from consumer for destination {}. Closing the connection...", destinationId);
                                emitter.onComplete();
                                return;
                            }

                            if (!(m instanceof TextMessage)) {
                                logger.error("Unsupported type of incoming message {}", m);
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

                            IncomingMessageMetaData meta = messageDetailsCreator.createIncomingMessageMetaData(m);
                            incomingMessage = new IncomingMessage<>(transportMessage, meta);
                            metricsCollector.messageReceived(destinationId);
                        }
                        emitter.onNext(incomingMessage);
                    },
                    consumer -> {
                        metricsCollector.subscriptionDestroyed(destinationId);
                        jmsObjectRepository.destroyConsumer(consumer);
                        mapDestinationIdToMessageStream.remove(destinationId);
                        logger.info("Closed connection to destination {}", destinationId);
                    }
            );
            return f.subscribeOn(Schedulers.io())
                    .share();
        });
    }

}
