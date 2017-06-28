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
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * Poller impl according to the Kafka JavaDoc
 */
public class KafkaPoller {
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Consumer<String,String> kafkaConsumer;
    private final long pollTimeout;
    private final TimeUnit pollTimeUnit;
    private java.util.function.Consumer<ConsumerRecords<String,String>> recordsConsumer;

    public KafkaPoller(Consumer<String,String> kafkaConsumer,
                       long pollTimeout, TimeUnit pollTimeUnit) {
        requireNonNull(kafkaConsumer, "kafkaConsumer must not be null");
        if (pollTimeout<=0) throw new IllegalArgumentException("pollTimeout must be greater than 0");
        requireNonNull(pollTimeUnit, "pollTimeUnit must not be null");
        this.kafkaConsumer = kafkaConsumer;
        this.pollTimeout = pollTimeout;
        this.pollTimeUnit = pollTimeUnit;
    }

    public void setRecordsConsumer(java.util.function.Consumer<ConsumerRecords<String,String>> recordsConsumer) {
        requireNonNull(recordsConsumer, "recordsConsumer must not be null");
        if (this.recordsConsumer!=null) {
            throw new RuntimeException("recordsConsumer has already been set!");
        }
        this.recordsConsumer = recordsConsumer;
    }

    protected ConsumerRecords<String,String> doPoll(long pollTimeout, TimeUnit pollTimeUnit) {
        return kafkaConsumer.poll(pollTimeUnit.toMillis(pollTimeout));
    }

    public void start() {
        requireNonNull(recordsConsumer,"KafkaPoller can only be started having a valid recordsConsumer");
        try {
            while (!shutdown.get()) {
                ConsumerRecords<String,String> records = doPoll(pollTimeout, pollTimeUnit);
                recordsConsumer.accept(records);
            }
        } catch (WakeupException e) {
            // Ignore exception if closing
            if (!shutdown.get()) throw e;
        } finally {
            kafkaConsumer.close();
        }
    }

    public void shutdown() {
        shutdown.set(true);
        kafkaConsumer.wakeup();
    }
}
