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
import io.reactivex.schedulers.Schedulers;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;

public class KafkaMessageReceiver<InternalMessageType>
        extends MessageReceiver<String, InternalMessageType, String, KafkaSpecificInfo> {

    private final Logger logger = LoggerFactory.getLogger(KafkaMessageReceiver.class);
    private final Properties consumerProperties;

    final Map<String, Flowable<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>>> topicToMessageStream;

    KafkaMessageReceiver(String identifier,
                         Properties consumerProperties,
                         MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                         Metrics metrics) {
        super(identifier, messageUnmarshaller, metrics);
        this.consumerProperties = consumerProperties;
        this.topicToMessageStream = new ConcurrentHashMap<>();
    }

    Consumer<String, String> consumerForTopic(String topic) {
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(consumerProperties);
        kafkaConsumer.subscribe(singletonList(topic));
        return kafkaConsumer;
    }


    public Flowable<InternalMessageType> unmarshall (ConsumerRecords<String, String> consumerRecords) {
        return Flowable.create(s -> {
            consumerRecords.forEach(record -> {
                try {
                    InternalMessageType internalMessage = messageUnmarshaller.unmarshal(record.value());
                    s.onNext(internalMessage);
                } catch (Throwable t) {
                    metricsCollector.unparsableMessageReceived(record.topic());
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

        long pollTimeout = 1; // TODO: configurable
        TimeUnit pollTimeUnit = TimeUnit.SECONDS;  // TODO: configurable

        return topicToMessageStream.computeIfAbsent(destination, key -> {
            Flowable<ConsumerRecords<String, String>> rawMessages = Flowable.generate(
                    () -> {
                        logger.info("Subscribing to " + destination);
                        metricsCollector.subscriptionCreated(destination);
                        return consumerForTopic(key);
                    },
                    (consumer, emitter) -> {
                        ConsumerRecords<String, String> consumerRecords = null;
                        do {
                            consumerRecords = consumer.poll(pollTimeUnit.toMillis(pollTimeout));
                        } while (consumerRecords == null);
                        // FIXME: additional condition to break in case of shutdown - check and call onComplete()
                        emitter.onNext(consumerRecords);
                    },
                    consumer -> {
                        topicToMessageStream.remove(destination);
                        metricsCollector.subscriptionDestroyed(destination);
                        consumer.close();
                        logger.info("Stopped listening to " + destination);
                    }
            );
            return rawMessages
                    .subscribeOn(Schedulers.io())
                    .concatMap(this::unmarshall)
                    .map(message -> {
                        // TODO: what kind of data is interesting for consumers?
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

    public void shutdown() {
        // TODO - how do we stop the message streams? Is this needed at all?
//        Set<String> topicsCurrentlyListenedTo = new HashSet<>(topicToMessageStream.keySet());
//        for (String topic: topicsCurrentlyListenedTo) {
//             destroyPollerForTopic(topic);
//        }
        topicToMessageStream.clear();
    }
}
