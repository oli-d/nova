/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics.kafka;

import ch.qos.logback.classic.Level;
import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.kafka.KafkaAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.charithe.kafka.EphemeralKafkaBroker;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class KafkaMetricsReporterTest {
    private static final int KAFKA_PORT = 11_000;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private EphemeralKafkaBroker kafkaBroker;
    private KafkaAdapter<Map> kafkaAdapter;
    private KafkaMetricsReporter sut;

    @BeforeAll
    static void initLogging() {
        Logger logger = LoggerFactory.getLogger("org.apache.kafka");
        ch.qos.logback.classic.Logger l2 = (ch.qos.logback.classic.Logger) logger;
        l2.setLevel(Level.WARN);
        logger = LoggerFactory.getLogger("kafka");
        l2 = (ch.qos.logback.classic.Logger) logger;
        l2.setLevel(Level.WARN);
        logger = LoggerFactory.getLogger("org.apache.zookeeper");
        l2 = (ch.qos.logback.classic.Logger) logger;
        l2.setLevel(Level.WARN);
    }

    @BeforeEach
    void setUp() throws Exception {
        kafkaBroker = EphemeralKafkaBroker.create(KAFKA_PORT);
        kafkaBroker.start().get();

        kafkaAdapter = KafkaAdapter.builder(Map.class)
                .setServerAddress("127.0.0.1:" + KAFKA_PORT)
                .setIdentifier("Test")
                .addProducerProperty(ProducerConfig.BATCH_SIZE_CONFIG, "1")
                .addConsumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .addConsumerProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                .build();

        sut = new KafkaMetricsReporter(kafkaAdapter, "test.metrics");
    }

    @Test
    void dumpIsTransmittedAsExpected() throws Exception {
        int numDumps = 3;

        CountDownLatch cdl = new CountDownLatch(numDumps);
        kafkaAdapter.messages("test.metrics").subscribe(dump -> cdl.countDown());

        Nova nova = Nova.builder().build();
        for (int i=0; i< numDumps; i++) {
            sut.accept(nova.metrics.dump());
            Thread.sleep(700);
        }

        cdl.await(20, TimeUnit.SECONDS);
        assertThat(cdl.getCount(), is(0L));
    }

}