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

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;
import io.reactivex.Single;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import static java.util.Objects.requireNonNull;

public class MessageSender extends ch.squaredesk.nova.comm.sending.MessageSender<Destination, String, OutgoingMessageMetaData> {
    private final JmsObjectRepository jmsObjectRepository;

    MessageSender(String identifier,
                            JmsObjectRepository jmsObjectRepository,
                            Metrics metrics) {
        super(identifier, metrics);
        this.jmsObjectRepository = jmsObjectRepository;
    }

    @Override
    public Single<OutgoingMessageMetaData> send(String message, OutgoingMessageMetaData meta) {
        requireNonNull(message, "message must not be null");

        try {
            TextMessage textMessage = jmsObjectRepository.createTextMessage();
            if (meta.details != null) {
                textMessage.setJMSCorrelationID(meta.details.correlationId);
                textMessage.setJMSReplyTo(meta.details.replyDestination);
                if (meta.details.customHeaders != null) {
                    for (String key : meta.details.customHeaders.keySet()) {
                        textMessage.setObjectProperty(key, meta.details.customHeaders.get(key));
                    }
                }
            }
            textMessage.setText(message);

            MessageProducer producer = jmsObjectRepository.createMessageProducer(meta.destination);
            if (meta.details == null) {
                producer.send(textMessage);
            } else {
                producer.send(textMessage,
                        meta.details.deliveryMode,
                        meta.details.priority,
                        meta.details.timeToLive);
            }
            return Single.just(meta.setJmsMessage(textMessage));
        } catch (Exception e) {
            return Single.error(e);
        }
    }

}
