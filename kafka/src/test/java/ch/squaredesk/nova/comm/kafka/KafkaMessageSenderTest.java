package ch.squaredesk.nova.comm.kafka;

import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.observers.TestObserver;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
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
                message -> message,
                new Metrics());
    }

    @Test
    void messageMarshallingErrorOnSendForwardedToSubscriber() {
        TestObserver observer = sut.doSend("myMessage",
                new MessageSendingInfo.Builder<String, KafkaSpecificInfo>()
                        .withDestination("dest")
                        .withTransportSpecificInfo(new KafkaSpecificInfo())
                        .build()).test();
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