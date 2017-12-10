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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class KafkaMessageReceiverTest {
    Properties consumerProps;
    Properties producerProps;
    KafkaMessageReceiver<String> sut;

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

        sut = new KafkaMessageReceiver<>("id", consumerProps, s->s, new Metrics());
    }


    @Test
    void pollerForTopicConsidersPassedProperties() throws Exception {
        Consumer<String, String> consumer = sut.consumerForTopic("pollerPropsTest");
        String clientId = getClientIdFrom(consumer);
        assertThat(clientId, is(consumerProps.getProperty("client.id")));
    }

    private String getClientIdFrom(Consumer<String, String> consumer) throws Exception {
        Field f = KafkaConsumer.class.getDeclaredField("clientId");
        f.setAccessible(true);

        return (String) f.get(consumer);
    }

    /*
    @Test
    void shutdownClosesAllOpenPollersAndProducers() throws Exception {
        // first, we create a few pollers
        int numPollers = 5;
        askSutForPollers(numPollers);
        assertThat(sut.topicToMessageStream.size(), is(numPollers));

        // and producers
        int numProducers = 5;
        askSutForProducers(numProducers);
        assertThat(sut.producers.size(), is(numProducers));

        // then we replace those objects with our mocks
        Set<String> topics = new HashSet<>(sut.topicToMessageStream.keySet());
        for (String topic: topics) {
            sut.topicToMessageStream.put(topic, new MyPollerMock());
        }
        sut.producers.clear();
        for (int i = 0; i < numProducers; i++) {
            sut.producers.add(new MyProducerMock());
        }

        // ask to shut down
        sut.shutdown();

        // and verify that shutdown shuts down each open poller and producer
        for (KafkaPoller mock: sut.topicToMessageStream.values()) {
            assertTrue(((MyPollerMock)mock).shutdown);
        }
        for (Producer mock: sut.producers) {
            assertTrue(((MyProducerMock)mock).shutdown);
        }

        // and clears the caches
        assertTrue(sut.topicToMessageStream.isEmpty());
        assertTrue(sut.producers.isEmpty());
    }
    */
}