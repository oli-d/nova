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

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class TestKafkaObjectFactory extends KafkaObjectFactory {
    private MockProducer<String, String> producer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());

    TestKafkaObjectFactory() {
        super(new Properties(), new Properties());
    }

    @Override
    public KafkaPoller pollerForTopic(String topic, long pollFrequency, TimeUnit pollFrequencyTimeUnit) {
        topicToPoller.put(topic, new TestKafkaPoller(topic));
        return topicToPoller.get(topic);
    }

    @Override
    public Producer<String, String> producer() {
        return producer;
    }

    TestKafkaPoller testPollerForTopic(String topic) {
        return (TestKafkaPoller) topicToPoller.get(topic);
    }

    MockProducer<String, String> mockProducer() {
        return producer;
    }
}
