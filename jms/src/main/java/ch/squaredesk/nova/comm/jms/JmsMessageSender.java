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
import ch.squaredesk.nova.comm.sending.MessageSender;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import static java.util.Objects.requireNonNull;

class JmsMessageSender<InternalMessageType> extends MessageSender<Destination, InternalMessageType, String, JmsSpecificInfo> {
    private final JmsObjectRepository jmsObjectRepository;

    JmsMessageSender(String identifier,
                     JmsObjectRepository jmsObjectRepository,
                     MessageMarshaller<InternalMessageType,String> messageMarshaller,
                     Metrics metrics) {
        super(identifier, messageMarshaller, metrics);
        this.jmsObjectRepository = jmsObjectRepository;
    }


    @Override
    public Completable doSend(String message, MessageSendingInfo<Destination, JmsSpecificInfo> sendingInfo) {
        requireNonNull(message, "message must not be null");
        return Completable.create(s -> {
            try {
                TextMessage textMessage = jmsObjectRepository.createTextMessage();
                if (sendingInfo.transportSpecificInfo != null) {
                    textMessage.setJMSCorrelationID(sendingInfo.transportSpecificInfo.correlationId);
                    textMessage.setJMSReplyTo(sendingInfo.transportSpecificInfo.replyDestination);
                    if (sendingInfo.transportSpecificInfo.customHeaders != null) {
                        for (String key : sendingInfo.transportSpecificInfo.customHeaders.keySet()) {
                            textMessage.setObjectProperty(key, sendingInfo.transportSpecificInfo.customHeaders.get(key));
                        }
                    }
                }
                textMessage.setText(message);

                MessageProducer producer = jmsObjectRepository.createMessageProducer(sendingInfo.destination);
                producer.send(textMessage,
                        sendingInfo.transportSpecificInfo.deliveryMode,
                        sendingInfo.transportSpecificInfo.priority,
                        sendingInfo.transportSpecificInfo.timeToLive);

                s.onComplete();
            } catch (Throwable t) {
                s.onError(t);
            }
        }).subscribeOn(JmsAdapter.jmsSubscriptionScheduler);
    }
}
