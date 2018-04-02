/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.kafka;

import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.sending.MessageSender;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

public class KafkaMessageSender<InternalMessageType> extends MessageSender<String, InternalMessageType, String, OutgoingMessageMetaData> {
    private final Producer<String, String> producer;

    KafkaMessageSender(String identifier,
                                 Properties producerProperties,
                                 MessageMarshaller<InternalMessageType, String> messageMarshaller,
                                 Metrics metrics) {
        super(identifier, messageMarshaller, metrics);
        this.producer = new KafkaProducer<>(producerProperties);
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
        ProducerRecord<String, String> record = new ProducerRecord<String, String>(sendingInfo.destination, messageAsText);
        return Completable.fromFuture(producer.send(record));
    }

}
