/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.kafka;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MessageReceiver
        extends ch.squaredesk.nova.comm.retrieving.MessageReceiver<String, String, IncomingMessageMetaData> {

    private final Logger logger = LoggerFactory.getLogger(MessageReceiver.class);
    private final Flowable<IncomingMessage<String, IncomingMessageMetaData>> allMessagesStream;
    private final Scheduler scheduler = Schedulers.io();
    private final Map<String, AtomicInteger> topicToSubscriptionCount = new ConcurrentHashMap<>();

    protected MessageReceiver(String identifier,
                              Properties consumerProperties,
                              Duration pollTimeout,
                              Metrics metrics) {
        super(Metrics.name("kafka", identifier).toString(), metrics);

        AtomicBoolean shutdown = new AtomicBoolean(false);

        Function<KafkaConsumer<String, String>, ConsumerRecords<String, String>> poller = consumer -> {
            ConsumerRecords<String, String> consumerRecords = null;
            do {
                try {
                    consumerRecords = consumer.poll(pollTimeout);
                } catch (Exception ex) {
                    break;
                }
                if (consumerRecords != null && consumerRecords.isEmpty()) {
                    logger.trace("Ignoring empty consumer records");
                }
            } while (consumerRecords == null && !shutdown.get());
            return consumerRecords;
        };

        Runnable sleeper = () -> {
            logger.trace("No topic subscribed yet, sleeping {}", pollTimeout);
            try {
                Thread.currentThread().sleep(pollTimeout.toMillis());
            } catch (InterruptedException e) {
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            }
        };

    BiFunction<Set<String>, Pair<KafkaConsumer<String, String>, HashSet<String>>, Boolean> subscriptionMaintainer =
                (subscribedTopics, consumerTopicsPair) -> {
                    if (!consumerTopicsPair._2.equals(subscribedTopics)) {
                        logger.debug("Changing topic subscriptions to " + subscribedTopics);
                        consumerTopicsPair._2.clear();
                        consumerTopicsPair._2.addAll(subscribedTopics);
                        consumerTopicsPair._1.subscribe(subscribedTopics);
                    }
                    return !consumerTopicsPair._2.isEmpty();
                };

        Flowable<ConsumerRecords<String, String>> consumerRecordsStream = Flowable.generate(
                () -> {
                    logger.info("Opening connection to Kafka broker");
                    return new Pair<>(new KafkaConsumer<String, String>(consumerProperties),
                            new HashSet<String>());
                },
                (consumerTopicsPair, emitter) -> {
                    while (!subscriptionMaintainer.apply(topicToSubscriptionCount.keySet(), consumerTopicsPair)) {
                        // nothing subscribed
                        sleeper.run();
                    }
                    ConsumerRecords<String, String> consumerRecords = poller.apply(consumerTopicsPair._1);
                    if (consumerRecords == null) {
                        // only happens, if shutdown was initiated
                        emitter.onComplete();
                    } else {
                        logger.trace("Read consumer records, size = {}", consumerRecords.count());
                        emitter.onNext(consumerRecords);
                    }
                },
                consumerTopicsPair -> {
                    logger.info("Shutting down connection to Kafka broker");
                    try {
                        consumerTopicsPair._1.close();
                    } catch (Exception e) {
                        logger.info("An error occurred trying to close KafkaConsumer", e.getCause());
                    }
                }
        );

        allMessagesStream = consumerRecordsStream
                .subscribeOn(scheduler)
                .flatMap(MessageReceiver::observableFor)
                .map(record -> {
                    metricsCollector.messageReceived(record.topic());
                    RetrieveInfo kafkaSpecificInfo = new RetrieveInfo();
                    IncomingMessageMetaData metaData = new IncomingMessageMetaData(record.topic(), kafkaSpecificInfo);
                    return new IncomingMessage<>(record.value(), metaData);
                })
                .share();
    }

    private static Flowable<ConsumerRecord<String, String>> observableFor (ConsumerRecords<String, String> records) {
        return Flowable.generate(
                records::iterator,
                (iterator, emitter) -> {
                    if (iterator.hasNext()) {
                        emitter.onNext(iterator.next());
                    } else {
                        emitter.onComplete();
                    }
                }
        );
    }

    @Override
    public Flowable<IncomingMessage<String, IncomingMessageMetaData>> messages(String destination) {
        Objects.requireNonNull(destination, "destination must not be null");

        return allMessagesStream
                .filter(incomingMessage -> destination.equals(incomingMessage.metaData.destination))
                .doOnSubscribe(s -> {
                    scheduler.scheduleDirect(() -> {
                        AtomicInteger subsCounter = topicToSubscriptionCount.computeIfAbsent(
                                destination,
                                key -> new AtomicInteger(0)
                        );
                        int count = subsCounter.incrementAndGet();
                        logger.info("Subscribing to topic {}, current subscription count is  {}", destination, count);
                        metricsCollector.subscriptionCreated(destination);
                    });
                })
                .doFinally(() -> {
                    scheduler.scheduleDirect(() -> {
                        metricsCollector.subscriptionDestroyed(destination);
                        AtomicInteger subsCounter = topicToSubscriptionCount.get(destination);
                        if (subsCounter == null) {
                            logger.error("WTF! Unsubscribing topic {} but the counter is gone?!?!?", destination);
                        } else {
                            int count = subsCounter.decrementAndGet();
                            if (count == 0) {
                                topicToSubscriptionCount.remove(destination);
                                logger.info("Unsubscribed last subscription to topic " + destination);
                            } else {
                                logger.info("Unsubscribed from topic {}, current subscription count is  {}", destination, count);
                            }
                        }
                    });
                });
    }

    public void shutdown() {
        logger.info("Shutting down, currently subscribed to " + topicToSubscriptionCount.keySet());
        topicToSubscriptionCount.clear();
//        allMessageStreamSubscription.dispose();
    }
}
