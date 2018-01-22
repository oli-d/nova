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
import ch.squaredesk.nova.comm.retrieving.MessageReceiver;
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

public class JmsMessageReceiver<InternalMessageType>
        extends MessageReceiver<Destination, InternalMessageType, String, JmsSpecificInfo> {

    private static final Logger logger = LoggerFactory.getLogger(JmsMessageReceiver.class);

    private final JmsObjectRepository jmsObjectRepository;

    private final Map<String, Flowable<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>>>
            mapDestinationIdToMessageStream = new ConcurrentHashMap<>();
    private final JmsMessageDetailsCreator messageDetailsCreator = new JmsMessageDetailsCreator();

    JmsMessageReceiver(String identifier,
                       JmsObjectRepository jmsObjectRepository,
                       MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                       Metrics metrics) {
        super(identifier, messageUnmarshaller, metrics);
        this.jmsObjectRepository = jmsObjectRepository;
    }

    @Override
    public Flowable<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>> messages(Destination destination) {
        Objects.requireNonNull(destination, "destination must not ne bull");

        String destinationId = jmsObjectRepository.idFor(destination);
        return mapDestinationIdToMessageStream.computeIfAbsent(destinationId, key -> {
            Flowable<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>> f = Flowable.generate(
                    () -> {
                        logger.info("Opening connection to destination " + destinationId);
                        metricsCollector.subscriptionCreated(destinationId);
                        return jmsObjectRepository.createMessageConsumer(destination);
                    },
                    (consumer, emitter) -> {
                        IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo> incomingMessage = null;
                        while (incomingMessage == null) {
                            Message m = null;
                            try {
                                m = consumer.receive();
                            } catch (Exception e) {
                                // noop, the consumer was closed
                            }

                            if (m == null) {
                                logger.info("Unable to receive message from consumer for destination " + destinationId + ". Closing the connection...");
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

                            IncomingMessageDetails<Destination, JmsSpecificInfo> messageDetails =
                                    messageDetailsCreator.createMessageDetailsFor(m);
                            incomingMessage = new IncomingMessage<>(internalMessage, messageDetails);
                            metricsCollector.messageReceived(destinationId);
                        }
                            emitter.onNext(incomingMessage);
                    },
                    consumer -> {
                        metricsCollector.subscriptionDestroyed(destinationId);
                        jmsObjectRepository.destroyConsumer(consumer);
                        mapDestinationIdToMessageStream.remove(destinationId);
                        logger.info("Closed connection to destination " + destinationId);
                    }
            );
            return f.subscribeOn(Schedulers.io())
                    .share();
        });
    }

}
