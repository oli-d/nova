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

import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import static java.util.Objects.requireNonNull;

public class MessageSender<InternalMessageType> extends ch.squaredesk.nova.comm.sending.MessageSender<Destination, InternalMessageType, String, OutgoingMessageMetaData> {
    private final JmsObjectRepository jmsObjectRepository;

    MessageSender(String identifier,
                            JmsObjectRepository jmsObjectRepository,
                            MessageMarshaller<InternalMessageType, String> messageMarshaller,
                            Metrics metrics) {
        super(identifier, messageMarshaller, metrics);
        this.jmsObjectRepository = jmsObjectRepository;
    }

    @Override
    public Completable doSend(InternalMessageType message, OutgoingMessageMetaData sendingInfo) {
        requireNonNull(message, "message must not be null");

        String messageAsText;
        try {
            messageAsText = messageMarshaller.marshal(message);
        } catch (Exception e) {
            // TODO: metric?
            return Completable.error(e);
        }

        return Completable.create(s -> {
            try {
                TextMessage textMessage = jmsObjectRepository.createTextMessage();
                if (sendingInfo.details != null) {
                    textMessage.setJMSCorrelationID(sendingInfo.details.correlationId);
                    textMessage.setJMSReplyTo(sendingInfo.details.replyDestination);
                    if (sendingInfo.details.customHeaders != null) {
                        for (String key : sendingInfo.details.customHeaders.keySet()) {
                            textMessage.setObjectProperty(key, sendingInfo.details.customHeaders.get(key));
                        }
                    }
                }
                textMessage.setText(messageAsText);

                MessageProducer producer = jmsObjectRepository.createMessageProducer(sendingInfo.destination);
                producer.send(textMessage,
                        sendingInfo.details.deliveryMode,
                        sendingInfo.details.priority,
                        sendingInfo.details.timeToLive);

                s.onComplete();
            } catch (Throwable t) {
                s.onError(t);
            }
        }).subscribeOn(JmsAdapter.jmsSubscriptionScheduler);
    }

}
