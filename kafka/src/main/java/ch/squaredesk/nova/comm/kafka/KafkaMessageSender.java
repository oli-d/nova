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

import ch.squaredesk.nova.comm.sending.MessageSender;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

class KafkaMessageSender<InternalMessageType> extends MessageSender<String, InternalMessageType, String, KafkaSpecificInfo> {
    private final Producer<String, String> producer;

    KafkaMessageSender(String identifier,
                       KafkaObjectFactory kafkaObjectFactory,
                       Function<InternalMessageType,String> messageMarshaller,
                       Metrics metrics) {
        super(identifier, messageMarshaller::apply, metrics);
        this.producer = kafkaObjectFactory.producer();
    }


    @Override
    public Completable doSend(String message, MessageSendingInfo<String, KafkaSpecificInfo> sendingInfo) {
        requireNonNull(message, "message must not be null");
        return Completable.create(s -> {
            try {
                ProducerRecord<String,String> record = new ProducerRecord<String, String>(sendingInfo.destination,message);
                producer.send(record);
                s.onComplete();
            } catch (Throwable t) {
                s.onError(t);
            }
        });
    }

}
