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

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;
import io.reactivex.Single;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

public class MessageSender extends ch.squaredesk.nova.comm.sending.MessageSender<String, String, OutgoingMessageMetaData> {
    private final Producer<String, String> producer;

    protected MessageSender(String identifier,
                            Properties producerProperties,
                            Metrics metrics) {
        super(identifier, metrics);
        this.producer = new KafkaProducer<>(producerProperties);
    }

    @Override
    public Single<OutgoingMessageMetaData> send(String message, OutgoingMessageMetaData sendingInfo) {
        requireNonNull(message, "message must not be null");
        return Single.fromFuture(producer.send(new ProducerRecord<>(sendingInfo.destination, message)))
                .map(producerRecord -> sendingInfo);
    }

}
