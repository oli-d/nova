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

import ch.qos.logback.classic.Level;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaPollerTest {
    private KafkaObjectFactory objectFactory = new KafkaObjectFactory(new Properties(), new Properties());
    private KafkaPoller sut;

    @BeforeAll
    static void initLogging() {
        Logger logger = LoggerFactory.getLogger("org.apache.kafka");
        ch.qos.logback.classic.Logger l2 = (ch.qos.logback.classic.Logger)logger;
        l2.setLevel(Level.INFO);
    }

    @BeforeEach
    void setup() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9999");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        objectFactory = new KafkaObjectFactory(props, props);
        sut = objectFactory.pollerForTopic("test", 150, TimeUnit.MILLISECONDS);
    }


    @Test
    void cannotBeInstantiatedWithoutKafkaConsumer() {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sut = new KafkaPoller(null, 1, TimeUnit.SECONDS));
        assertThat(throwable.getMessage(), is("kafkaConsumer must not be null"));
    }

    @Test
    void cannotBeInstantiatedWithNegativePollTimeout() {
        Throwable throwable = assertThrows(IllegalArgumentException.class,
                () -> sut = objectFactory.pollerForTopic("test", -1, TimeUnit.SECONDS));
        assertThat(throwable.getMessage(), is("pollFrequency must be greater than zero"));
    }

    @Test
    void cannotBeInstantiatedWithZeroPollTimeout() {
        Throwable throwable = assertThrows(IllegalArgumentException.class,
                () -> sut = objectFactory.pollerForTopic("test", 0, TimeUnit.SECONDS));
        assertThat(throwable.getMessage(), is("pollFrequency must be greater than zero"));
    }

    @Test
    void cannotBeInstantiatedWithoutTimeUnit() {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sut = objectFactory.pollerForTopic("test", 1, null));
        assertThat(throwable.getMessage(), is("timeUnit must not be null"));
    }

    @Test
    void recordsConsumerMustBeSetBeforeStart() {
        Throwable throwable = assertThrows(RuntimeException.class, () -> sut.start());
        assertThat(throwable.getMessage(), containsString("having a valid recordsConsumer"));
    }

    @Test
    void recordsConsumerCannotBeChanged() {
        sut.setRecordsConsumer(records -> {});
        Throwable throwable = assertThrows(RuntimeException.class, () -> sut.setRecordsConsumer(records -> {}));
        assertThat(throwable.getMessage(), containsString("recordsConsumer has already been set"));
    }

    @Test
    void recordsConsumerCannotBeSetToNull() {
        Throwable throwable = assertThrows(NullPointerException.class, () -> sut.setRecordsConsumer(null));
        assertThat(throwable.getMessage(), containsString("recordsConsumer must not be null"));
    }

}