/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
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

import static java.util.Collections.singletonList;

class KafkaObjectFactory {
    private final Properties consumerProperties;
    private final Properties producerProperties;
    protected final Set<Producer<String, String>> producers;

    public KafkaObjectFactory(Properties consumerProperties, Properties producerProperties) {
        this.consumerProperties = consumerProperties;
        this.producerProperties = producerProperties;
        producers = new CopyOnWriteArraySet<>();
    }

    // TODO: is it ok to subscribe multiple times to the same topic???
    Consumer<String, String> consumerForTopic(String topic) {
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(consumerProperties);
        kafkaConsumer.subscribe(singletonList(topic));
        return kafkaConsumer;
    }


    Producer<String,String> producer() {
        Producer<String, String> p = new KafkaProducer<>(producerProperties);
        producers.add(p);
        return p;
    }

}
