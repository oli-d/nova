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
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


class KafkaMessageSenderTest {
    private KafkaMessageSender<String> sut;

    @BeforeEach
    void setup() {
        sut = new KafkaMessageSender<>(
                "myId",
                new MyObjectFactory(),
                message -> { throw new RuntimeException("for test"); },
                new Metrics());
    }

    @Test
    void messageMarshallingErrorOnSendForwardedToSubscriber() throws Exception {
        Completable completable = sut.sendMessage("dest","myMessage", new KafkaSpecificInfo());
        TestObserver<Void> observer = completable.test();
        observer.await();
        observer.assertError(RuntimeException.class);
        observer.assertErrorMessage("for test");
    }

    private class MyObjectFactory extends KafkaObjectFactory {

        MyObjectFactory() {
            super(new Properties(), new Properties());
        }

        @Override
        public Producer<String, String> producer() {
            return new Producer<String, String>() {
                @Override
                public void initTransactions() {

                }

                @Override
                public void beginTransaction() throws ProducerFencedException {

                }

                @Override
                public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, String consumerGroupId) throws ProducerFencedException {

                }

                @Override
                public void commitTransaction() throws ProducerFencedException {

                }

                @Override
                public void abortTransaction() throws ProducerFencedException {

                }

                @Override
                public Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                    throw new RuntimeException("for test");
                }

                @Override
                public Future<RecordMetadata> send(ProducerRecord<String, String> record, Callback callback) {
                    return null;
                }

                @Override
                public void flush() {
                }

                @Override
                public List<PartitionInfo> partitionsFor(String topic) {
                    return null;
                }

                @Override
                public Map<MetricName, ? extends Metric> metrics() {
                    return null;
                }

                @Override
                public void close() {
                }

                @Override
                public void close(long timeout, TimeUnit unit) {
                }
            };
        }
    }
}