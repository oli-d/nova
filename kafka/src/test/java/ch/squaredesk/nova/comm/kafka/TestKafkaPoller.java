package ch.squaredesk.nova.comm.kafka;


import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class TestKafkaPoller extends KafkaPoller {
    private LinkedBlockingQueue<ConsumerRecords<String, String>> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicLong messageCount = new AtomicLong();
    private final String topic;

    TestKafkaPoller(String topic) {
        super(new StubbedConsumer(), 125, TimeUnit.MILLISECONDS);
        this.topic = topic;
    }

    @Override
    protected ConsumerRecords<String, String> doPoll(long pollTimeout, TimeUnit pollTimeUnit) {
        try {
            return queue.poll(pollTimeout, pollTimeUnit);
        } catch (InterruptedException e) {
            return null;
        }
    }

    void injectMessage(String message)  {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, messageCount.getAndIncrement(), null, message);
        Map<TopicPartition, List<ConsumerRecord<String, String>>> map = new ConcurrentHashMap<>();
        map.put(new TopicPartition(topic, 0), Collections.singletonList(record));
        ConsumerRecords<String, String> records = new ConsumerRecords<>(map);
        try {
            queue.put(records);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
