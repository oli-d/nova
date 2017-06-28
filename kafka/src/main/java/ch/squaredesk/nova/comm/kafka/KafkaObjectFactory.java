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

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

public class KafkaObjectFactory {
    private final Properties consumerProperties;
    private final Properties producerProperties;
    final Map<String, KafkaPoller> topicToPoller;
    protected final Set<Producer<String, String>> producers;

    public KafkaObjectFactory(Properties consumerProperties, Properties producerProperties) {
        this.consumerProperties = consumerProperties;
        this.producerProperties = producerProperties;
        topicToPoller = new ConcurrentHashMap<>();
        producers = new CopyOnWriteArraySet<>();
    }

    private Consumer<String, String> consumerForTopic(String topic) {
        KafkaConsumer kafkaConsumer = new KafkaConsumer<>(consumerProperties);
        kafkaConsumer.subscribe(Arrays.asList(topic));
        return kafkaConsumer;
    }

    public KafkaPoller pollerForTopic(String topic, long pollFrequency, TimeUnit pollFrequencyTimeUnit) {
        return topicToPoller.computeIfAbsent(topic, key -> new KafkaPoller(consumerForTopic(key), pollFrequency, pollFrequencyTimeUnit));
    }

    public void destroyPollerForTopic(String topic) {
        KafkaPoller poller =  topicToPoller.remove(topic);
        if (poller!=null) {
            poller.shutdown();
        }
    }

    public Producer<String,String> producer() {
        Producer<String, String> p = new KafkaProducer<>(producerProperties);
        producers.add(p);
        return p;
    }

    public void shutdown() {
        Set<String> topicsCurrentlyListenedTo = new HashSet<>(topicToPoller.keySet());
        for (String topic: topicsCurrentlyListenedTo) {
            destroyPollerForTopic(topic);
        }

        new HashSet<>(producers).forEach(p -> {
            p.close(1, TimeUnit.SECONDS);
            producers.remove(p);
        });
    }
}
