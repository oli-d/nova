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

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaObjectFactoryTest {
    private KafkaObjectFactory sut;
    private Properties consumerProps;
    private Properties producerProps;

    // FIXME
    /*
    @BeforeEach
    void setup() {
        consumerProps = new Properties();
        consumerProps.put("bootstrap.servers","127.0.0.1:8888");
        consumerProps.put("key.deserializer",StringDeserializer.class.getName());
        consumerProps.put("value.deserializer", StringDeserializer.class.getName());
        consumerProps.put("client.id", UUID.randomUUID().toString());
        producerProps = new Properties();
        producerProps.put("bootstrap.servers","127.0.0.1:8888");
        producerProps.put("key.serializer",StringSerializer.class.getName());
        producerProps.put("value.serializer", StringSerializer.class.getName());
        producerProps.put("client.id", UUID.randomUUID().toString());

        sut = new KafkaObjectFactory(consumerProps, producerProps);
    }

    @Test
    void pollerForTopicConsidersPassedProperties() throws Exception {
        KafkaPoller kp = sut.pollerForTopic("pollerPropsTest", 1, TimeUnit.SECONDS);
        String clientId = getClientIdFrom(kp);
        assertThat(clientId, is(consumerProps.getProperty("client.id")));
    }

    @Test
    void producerAlwaysReturnsNewInstance()  {
        Producer p1 = sut.producer();
        Producer p2 = sut.producer();
        assertThat(p1, not(sameInstance(p2)));
    }

    @Test
    void producerConsidersPassedProperties()  throws Exception {
        Producer p = sut.producer();
        assertTrue(p instanceof KafkaProducer);
        ProducerConfig producerConfig = getProducerConfigFrom(p);
        assertThat(producerConfig.getString("client.id"), is(producerProps.get("client.id")));
    }

    private ProducerConfig getProducerConfigFrom (Producer p) throws Exception {
        Field f = KafkaProducer.class.getDeclaredField("producerConfig");
        f.setAccessible(true);
        return (ProducerConfig)f.get(p);
    }

    private String getClientIdFrom (KafkaPoller poller) throws Exception {
        Field f = KafkaPoller.class.getDeclaredField("kafkaConsumer");
        f.setAccessible(true);
        KafkaConsumer kafkaConsumer = (KafkaConsumer)f.get(poller);
        f = KafkaConsumer.class.getDeclaredField("clientId");
        f.setAccessible(true);

        return (String)f.get(kafkaConsumer);
    }

    private void askSutForPollers(int numPollers) throws Exception {
        String[] topics = new String[numPollers];
        for (int i = 0; i < topics.length; i++) {
            topics[i] = "shutdownTestTopic" + i;
        }
        for (String topic: topics)  sut.pollerForTopic(topic, 1, TimeUnit.HOURS);
    }

    private void askSutForProducers(int numProducers) throws Exception {
        for (int i = 0; i < numProducers; i++) {
            sut.producer();
        }
    }

    /*
    @Test
    void shutdownClosesAllOpenPollersAndProducers() throws Exception {
        // first, we create a few pollers
        int numPollers = 5;
        askSutForPollers(numPollers);
        assertThat(sut.topicToPoller.size(), is(numPollers));

        // and producers
        int numProducers = 5;
        askSutForProducers(numProducers);
        assertThat(sut.producers.size(), is(numProducers));

        // then we replace those objects with our mocks
        Set<String> topics = new HashSet<>(sut.topicToPoller.keySet());
        for (String topic: topics) {
            sut.topicToPoller.put(topic, new MyPollerMock());
        }
        sut.producers.clear();
        for (int i = 0; i < numProducers; i++) {
            sut.producers.add(new MyProducerMock());
        }

        // ask to shut down
        sut.shutdown();

        // and verify that shutdown shuts down each open poller and producer
        for (KafkaPoller mock: sut.topicToPoller.values()) {
            assertTrue(((MyPollerMock)mock).shutdown);
        }
        for (Producer mock: sut.producers) {
            assertTrue(((MyProducerMock)mock).shutdown);
        }

        // and clears the caches
        assertTrue(sut.topicToPoller.isEmpty());
        assertTrue(sut.producers.isEmpty());
    }
    */
}