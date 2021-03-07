/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MessageSenderTest {
    Properties producerProps;
    MessageSender sut;

    @BeforeEach
    void setup() {
        producerProps = new Properties();
        producerProps.put("bootstrap.servers","127.0.0.1:8888");
        producerProps.put("key.serializer",StringSerializer.class.getName());
        producerProps.put("value.serializer", StringSerializer.class.getName());
        producerProps.put("client.id", UUID.randomUUID().toString());

        sut = new MessageSender("ID", producerProps);
    }

    @Test
    void sutConsidersPassedProperties()  throws Exception {
        ProducerConfig producerConfig = getProducerConfigFrom(sut);
        assertThat(producerConfig.getString("client.id"), is(producerProps.get("client.id")));
    }

    private static ProducerConfig getProducerConfigFrom (MessageSender messageSender) throws Exception {
        Field f = MessageSender.class.getDeclaredField("producer");
        f.setAccessible(true);
        Producer p = (Producer)f.get(messageSender);

        f = KafkaProducer.class.getDeclaredField("producerConfig");
        f.setAccessible(true);
        return (ProducerConfig)f.get(p);
    }

}