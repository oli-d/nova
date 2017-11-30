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

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageDetails;
import ch.squaredesk.nova.comm.retrieving.MessageReceiver;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class KafkaMessageReceiver<InternalMessageType>
        extends MessageReceiver<String, InternalMessageType, String, KafkaSpecificInfo> {

    private final Logger logger = LoggerFactory.getLogger(KafkaMessageReceiver.class);

    final Map<String, Flowable<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>>> topicToPoller;

    private final KafkaObjectFactory kafkaObjectFactory;

    KafkaMessageReceiver(String identifier,
                         KafkaObjectFactory kafkaObjectFactory,
                         MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                         Metrics metrics) {
        super(identifier, messageUnmarshaller, metrics);
        this.kafkaObjectFactory = kafkaObjectFactory;
        this.topicToPoller = new ConcurrentHashMap<>();
    }

    public Flowable<InternalMessageType> unmarshall (ConsumerRecords<String, String> consumerRecords) {
        return Flowable.create(s -> {
            consumerRecords.forEach(record -> {
                try {
                    InternalMessageType internalMessage = messageUnmarshaller.unmarshal(record.value());
                    s.onNext(internalMessage);
                } catch (Throwable t) {
                    // FIXME: metricsCollector.unparsableMessageReceived(destination, record.value());
                    logger.error("Unable to parse incoming message " + record, t);
                }
            });
            s.onComplete();
        }, BackpressureStrategy.BUFFER);
    }

    @Override
    public Flowable<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>> messages (String destination) {
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(messageUnmarshaller, "unmarshaller must not be null");

        long pollTimeout = 1; // FIXME: field
        TimeUnit pollTimeUnit = TimeUnit.SECONDS;  // FIXME: field

        return topicToPoller.computeIfAbsent(destination, key -> {
            Flowable<ConsumerRecords<String, String>> rawMessages = Flowable.generate(
                    () -> kafkaObjectFactory.consumerForTopic(key),
                    (consumer, emitter) -> {
                        ConsumerRecords<String, String> consumerRecords = null;
                        do {
                            consumerRecords = consumer.poll(pollTimeUnit.toMillis(pollTimeout));
                        } while (consumerRecords == null);
                        // FIXME: additional condition to break in case of shutdown - check and call onComplete()
                        emitter.onNext(consumerRecords);
                    },
                    consumer -> {
                        topicToPoller.remove(destination);
                        consumer.close();
                    }
            );
            return rawMessages
                    .subscribeOn(Schedulers.io())
                    .concatMap(this::unmarshall)
                    .map(message -> {
                        // FIXME: which data?
                        KafkaSpecificInfo kafkaSpecificInfo = new KafkaSpecificInfo();
                        IncomingMessageDetails<String, KafkaSpecificInfo> messageDetails = new IncomingMessageDetails.Builder<String, KafkaSpecificInfo>()
                                .withDestination(destination)
                                .withTransportSpecificDetails(kafkaSpecificInfo)
                                .build();

                        return new IncomingMessage<>(message, messageDetails);
                    })
                    .share();
        });
    }

    public void destroyPollerForTopic(String topic) {
        // FIXME:
    }

    public void shutdown() {
        // FIXME - to be implemented
        Set<String> topicsCurrentlyListenedTo = new HashSet<>(topicToPoller.keySet());
        for (String topic: topicsCurrentlyListenedTo) {
            destroyPollerForTopic(topic);
        }

        /*
        new HashSet<>(producers).forEach(p -> {
            p.close(1, TimeUnit.SECONDS);
            producers.remove(p);
        });
        */
    }
}
