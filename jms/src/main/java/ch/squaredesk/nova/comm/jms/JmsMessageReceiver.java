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
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

public class JmsMessageReceiver<InternalMessageType>
        extends MessageReceiver<Destination, InternalMessageType, String, JmsSpecificInfo> {

    private final Logger logger = LoggerFactory.getLogger(JmsMessageReceiver.class);

    private final JmsMessageDetailsCreator messageDetailsCreator;
    private final JmsObjectRepository jmsObjectRepository;

    JmsMessageReceiver(String identifier,
                       JmsObjectRepository jmsObjectRepository,
                       MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                       Metrics metrics) {
        super(identifier, messageUnmarshaller, metrics);
        this.jmsObjectRepository = jmsObjectRepository;
        this.messageDetailsCreator = new JmsMessageDetailsCreator();
    }


    @Override
    public Observable<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>>
        doSubscribe(Destination destination) {

        return Observable.<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>>create(subscription -> {
            logger.info("Creating connection to " + destination);
            try {
                MessageConsumer consumer = jmsObjectRepository.createMessageConsumer(destination);
                if (consumer.getMessageListener() != null) {
                    logger.warn("Destination " + destination + " already wired up with listener, not doing anything!?!?");
                } else {
                    consumer.setMessageListener(jmsMessage -> {
                        if (!(jmsMessage instanceof TextMessage)) {
                            logger.error("Unsupported type of incoming message " + jmsMessage.getClass());
                            return;
                        }

                        String transportMessage;
                        try {
                            transportMessage = ((TextMessage) jmsMessage).getText();
                        } catch (Exception e) {
                            logger.error("Unable to read incoming message " + jmsMessage, e);
                            return;
                        }

                        InternalMessageType internalMessage;
                        try {
                            internalMessage = messageUnmarshaller.unmarshal(transportMessage);
                        } catch (Exception e) {
                            logger.error("Unable to unmarshal incoming message " + transportMessage, e);
                            return;
                        }

                        IncomingMessageDetails<Destination, JmsSpecificInfo> messageDetails =
                                messageDetailsCreator.createMessageDetailsFor(jmsMessage);
                        IncomingMessage<InternalMessageType,Destination,JmsSpecificInfo> incomingMessage =
                                new IncomingMessage<>(internalMessage,messageDetails);

                        subscription.onNext(incomingMessage);
                    });
                }
            } catch (Throwable t) {
                subscription.onError(t);
            }
        }).subscribeOn(JmsCommAdapter.jmsSubscriptionScheduler);

        // FIXME: threading


    }

    @Override
    protected void doUnsubscribe(Destination destination)  {
        try {
            jmsObjectRepository.destroyMessageConsumer(destination);
        } catch (JMSException e) {
            logger.error("Error, trying to unsubscribe from destination " + destination, e);
        }
    }

}
