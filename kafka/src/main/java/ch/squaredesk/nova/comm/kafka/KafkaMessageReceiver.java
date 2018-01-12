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
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

public class KafkaMessageReceiver<InternalMessageType>
        extends MessageReceiver<String, InternalMessageType, String, KafkaSpecificInfo> {

    private final Logger logger = LoggerFactory.getLogger(KafkaMessageReceiver.class);
    private final Flowable<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>> allMessagesStream;
    private final Disposable allMessageStreamSubscription;
    private final Scheduler scheduler = Schedulers.io();

    final Map<String, Flowable<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>>> topicToMessageStream = new ConcurrentHashMap<>();
    final AtomicBoolean topicSubscriptionsChanged = new AtomicBoolean(false);
    KafkaMessageReceiver(String identifier,
                         Properties consumerProperties,
                         MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                         Metrics metrics) {
        super(identifier, messageUnmarshaller, metrics);

        long pollTimeout = 1; // TODO: configurable
        TimeUnit pollTimeUnit = TimeUnit.SECONDS;  // TODO: configurable
        AtomicBoolean shutdown = new AtomicBoolean(false);

        Function<KafkaConsumer<String, String>, ConsumerRecords<String, String>> poller = consumer -> {
            ConsumerRecords<String, String> consumerRecords = null;
            do {
                try {
                    consumerRecords = consumer.poll(pollTimeUnit.toMillis(pollTimeout));
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
            logger.trace("No topic subscribed yet, sleeping {} {}", pollTimeout, pollTimeUnit);
            try {
                Thread.currentThread().sleep(pollTimeUnit.toMillis(pollTimeout));
            } catch (InterruptedException e) {
                // ignored
            }
        };

        BiFunction<Set<String>, KafkaConsumer<String, String>, Boolean> subscriptionMaintainer = (subscribedTopics, consumer) -> {
            if (topicSubscriptionsChanged.getAndSet(false)) {
                logger.debug("Changing topic subscriptions to " + subscribedTopics);
                consumer.subscribe(subscribedTopics);
            }
            return !subscribedTopics.isEmpty();
        };

        Flowable<ConsumerRecords<String, String>> consumerRecordsStream = Flowable.generate(
                () -> {
                    logger.info("Opening connection to Kafka broker");
                    return new KafkaConsumer<String, String>(consumerProperties);
                },
                (consumer, emitter) -> {
                    while (!subscriptionMaintainer.apply(topicToMessageStream.keySet(), consumer)) {
                        // nothing subscribed
                        sleeper.run();
                    }
                    ConsumerRecords<String, String> consumerRecords = poller.apply(consumer);
                    if (consumerRecords == null) {
                        // only happens, if shutdown was initiated
                        emitter.onComplete();
                    } else {
                        logger.trace("Read consumer records, size = {}", consumerRecords.count());
                        emitter.onNext(consumerRecords);
                    }
                },
                consumer -> {
                    logger.info("Shutting down connection to Kafka broker");
                    try {
                        consumer.close();
                    } catch (Exception e) {
                        logger.info("An error occurred trying to close KafkaConsumer", e.getCause());
                    }
                }
        );
        allMessagesStream = consumerRecordsStream
                .subscribeOn(scheduler)
                .concatMap(this::unmarshall)
                .map(topicAndMessage -> {
                    // TODO: what kind of data is interesting for consumers?
                    KafkaSpecificInfo kafkaSpecificInfo = new KafkaSpecificInfo();
                    IncomingMessageDetails<String, KafkaSpecificInfo> messageDetails = new IncomingMessageDetails.Builder<String, KafkaSpecificInfo>()
                            .withDestination(topicAndMessage._1)
                            .withTransportSpecificDetails(kafkaSpecificInfo)
                            .build();

                    return new IncomingMessage<>(topicAndMessage._2, messageDetails);
                })
                .share();

        // we eagerly create the subscription to the broker
        allMessageStreamSubscription = allMessagesStream.subscribe();
    }

    Flowable<Pair<String, InternalMessageType>> unmarshall (ConsumerRecords<String, String> consumerRecords) {
        return Flowable.create(s -> {
            consumerRecords.forEach(record -> {
                try {
                    InternalMessageType internalMessage = messageUnmarshaller.unmarshal(record.value());
                    s.onNext(new Pair<>(record.topic(), internalMessage));
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

        return topicToMessageStream.computeIfAbsent(
                destination,
                key -> {
                    logger.debug("Creating new stream for topic " + destination);
                    return allMessagesStream
                            .filter(incomingMessage -> destination.equals(incomingMessage.details.destination))
                            .doOnSubscribe(s -> {
                                scheduler.scheduleDirect(() -> {
                                    topicSubscriptionsChanged.set(true);
                                    logger.info("Subscribing to topic " + destination);
                                    metricsCollector.subscriptionCreated(destination);
                                });
                            })
                            .doFinally(() -> {
                                scheduler.scheduleDirect(() -> {
                                    topicToMessageStream.remove(destination);
                                    metricsCollector.subscriptionDestroyed(destination);
                                    topicSubscriptionsChanged.set(true);
                                    logger.info("Unsubscribed from topic " + destination);
                                });
                            })
                            .share();
                }
        );
    }

    public void shutdown() {
        logger.info("Shutting down, currently subscribed to " + topicToMessageStream.keySet());
        topicToMessageStream.clear();
        allMessageStreamSubscription.dispose();
    }
}
