package ch.squaredesk.nova.comm.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class StubbedProducer implements Producer<String, String> {
    @Override
    public Future<RecordMetadata> send(ProducerRecord<String, String> record) {
        return null;
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
}
