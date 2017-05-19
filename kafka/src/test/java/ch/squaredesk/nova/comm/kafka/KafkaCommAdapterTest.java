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
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class KafkaCommAdapterTest {
    private TestKafkaObjectFactory kafkaObjectFactory;
    private KafkaCommAdapter<String> sut;

    @BeforeEach
    void setUp() throws Exception {
        kafkaObjectFactory = new TestKafkaObjectFactory();

        sut = KafkaCommAdapter.<String>builder()
                .setMessageMarshaller(message -> message)
                .setMessageUnmarshaller(message -> message)
                .setKafkaObjectFactory(kafkaObjectFactory)
                .setMetrics(new Metrics())
                .setIdentifier("Test")
                .build();
    }

    Logger logger = LoggerFactory.getLogger(KafkaCommAdapterTest.class);

    // @After
    void tearDown() throws Exception {
        logger.error("====> TEARING DOWN");
        sut.shutdown();
    }

    @Test
    void subscribeWithNullDestinationEagerlyThrows() throws Exception {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sut.messages(null));
        assertThat(throwable.getMessage(), containsStringIgnoringCase("destination"));
    }

    private TestKafkaPoller getTestPoller(String topic) {
        return getTestPoller(topic, 2, SECONDS);
    }

    private TestKafkaPoller getTestPoller(String topic, long timeout, TimeUnit timeUnit) {
        long maxTime = System.nanoTime() + timeUnit.toNanos(timeout);
        TestKafkaPoller result;
        do {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // noop
            }
            result = kafkaObjectFactory.testPollerForTopic(topic);
        } while (result == null && System.nanoTime() < maxTime);

        if (result!=null) {
            return result;
        } else {
            fail("Unable to retrieve MockConsumer for topic " + topic + " within " + timeout + " " + timeUnit.toString().toLowerCase());
            return null;
        }
    }

    @Test
    void subscriptionWorks() throws Exception {
        String topic = ("topic4SubsTest");
        CountDownLatch[] cdlHolder = { new CountDownLatch(2) };
        List<String> messages = new ArrayList<>();
        Disposable subscription1 = sut.messages(topic).subscribe(
            x -> {
                messages.add(x);
                cdlHolder[0].countDown();
            }
        );

        TestKafkaPoller poller = getTestPoller(topic);

        // send two messages and assure they were received by the subscriber
        poller.injectMessage("One");
        poller.injectMessage("Two");

        cdlHolder[0].await(10, SECONDS);
        assertThat(cdlHolder[0].getCount(),is(0L));
        assertThat(messages, contains("One", "Two"));

        // dispose the subscription, resubscribe and send another message
        subscription1.dispose();
        cdlHolder[0] = new CountDownLatch(99);
        CountDownLatch[] cdlHolder2 = { new CountDownLatch(1) };
        List<String> messages2 = new ArrayList<>();
        Disposable subscription2 = sut.messages(topic).subscribe(x -> {
            messages2.add(x);
            cdlHolder2[0].countDown();
        });

        // ensure that only the second subscription was invoked
        poller = getTestPoller(topic); // since we unsubscribed and subscribed, this is a new instance
        poller.injectMessage("Three");

        cdlHolder2[0].await(1, SECONDS);
        assertThat(cdlHolder[0].getCount(), is(99L));
        assertThat(cdlHolder2[0].getCount(), is(0L));
        assertThat(messages, contains("One", "Two"));
        assertThat(messages2, contains("Three"));

        subscription2.dispose();
    }

    @Test
    void multipleSubscribersSupportedOnSingleQueue() throws Exception {
        String topic = "multipleSubscriberTopic";
        List<String> valuesSubscriber1, valuesSubscriber2, valuesSubscriber3;
        valuesSubscriber1 = new ArrayList<>();
        valuesSubscriber2 = new ArrayList<>();
        valuesSubscriber3 = new ArrayList<>();
        CountDownLatch[] cdlHolder = new CountDownLatch[3];
        cdlHolder[0] = new CountDownLatch(1);
        cdlHolder[1] = new CountDownLatch(1);
        sut.messages(topic).subscribe(x -> {
            valuesSubscriber1.add(x);
            cdlHolder[0].countDown();
        });
        sut.messages(topic).subscribe(x -> {
            valuesSubscriber2.add(x);
            cdlHolder[1].countDown();
        });

        TestKafkaPoller poller = getTestPoller(topic);
        poller.injectMessage("msg1");
        cdlHolder[0].await(1, SECONDS);
        assertThat(cdlHolder[0].getCount(),is(0L));
        cdlHolder[1].await(1, SECONDS);
        assertThat(cdlHolder[1].getCount(),is(0L));
        assertThat(valuesSubscriber1.size(),is(1));
        assertThat(valuesSubscriber1,contains("msg1"));
        assertThat(valuesSubscriber2.size(),is(1));
        assertThat(valuesSubscriber2,contains("msg1"));

        sut.messages(topic).subscribe(x -> {
            valuesSubscriber3.add(x);
            cdlHolder[2].countDown();
        });
        cdlHolder[0] = new CountDownLatch(1);
        cdlHolder[1] = new CountDownLatch(1);
        cdlHolder[2] = new CountDownLatch(1);
        poller.injectMessage("msg2");

        cdlHolder[0].await(1, SECONDS);
        assertThat(cdlHolder[0].getCount(), is(0L));
        cdlHolder[1].await(1, SECONDS);
        assertThat(cdlHolder[1].getCount(), is(0L));
        cdlHolder[2].await(1, SECONDS);
        assertThat(cdlHolder[2].getCount(), is(0L));
        assertThat(valuesSubscriber1.size(), is(2));
        assertThat(valuesSubscriber1, contains("msg1", "msg2"));
        assertThat(valuesSubscriber2.size(), is(2));
        assertThat(valuesSubscriber2, contains("msg1", "msg2"));
        assertThat(valuesSubscriber3.size(), is(1));
        assertThat(valuesSubscriber3, contains("msg2"));
    }

    @Test
    void errorInMessageHandlingKillsSubscription() throws InterruptedException {
        String topic = ("topic4SubsErrorTest");
        CountDownLatch cdl = new CountDownLatch(3);
        List<Integer> messages = new ArrayList<>();
        sut.messages(topic).subscribe(
                x -> {
                    try {
                        messages.add(Integer.parseInt(x));
                    } finally {
                        cdl.countDown();
                    }
                }
        );

        TestKafkaPoller poller = getTestPoller(topic);

        // send two good and one bad message
        poller.injectMessage("1");
        poller.injectMessage("Two");
        poller.injectMessage("3");

        cdl.await(1, SECONDS); // 1 second, to make sure that "3" would be delivered in case the subs is still alive
        assertThat(cdl.getCount(),is(1L)); // should have NOT seen "3"
        assertThat(messages, contains(1));
    }

    @Test
    void sendNullMessageEagerlyThrows() {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sut.sendMessage("not used", null));
        assertThat(throwable.getMessage(), startsWith("message"));
    }

    @Test
    void sendMessageOnNullQueueEagerlyThrows() {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sut.sendMessage(null, "message"));
        assertThat(throwable.getMessage(), startsWith("destination"));
    }

    @Test
    void sendMessage() throws Exception {
        String topic = "topicForSendTest";
        sut.sendMessage(topic, "One").subscribe();
        sut.sendMessage(topic, "Two").subscribe();
        sut.sendMessage(topic, "Three").subscribe();

        ProducerRecord[] producerRecords = kafkaObjectFactory.mockProducer().history().toArray(new ProducerRecord[0]);
        stream(producerRecords).forEach(pr -> assertThat(pr.topic(), is(topic)));
        stream(producerRecords).forEach(pr -> assertNull(pr.key()));
        assertTrue(stream(producerRecords).map(pr -> pr.value()).anyMatch(val -> val.equals("One")));
        assertTrue(stream(producerRecords).map(pr -> pr.value()).anyMatch(val -> val.equals("Two")));
        assertTrue(stream(producerRecords).map(pr -> pr.value()).anyMatch(val -> val.equals("Three")));
    }

    @Test
    void sendingCanBeDoneFromMultipleThreads() throws Exception {
        String topic = "topicForMultiThreadSendTest";
        class Sender extends Thread {
            private final String id;

            public Sender(String id) {
                super("MultiThreadedSendTestSender-" + id);
                this.id = id;
            }

            @Override
            public void run() {
                CountDownLatch cdl = new CountDownLatch(3);
                sut.sendMessage(topic, "One-" + id).subscribe(() -> cdl.countDown());
                sut.sendMessage(topic, "Two-" + id).subscribe(() -> cdl.countDown());
                sut.sendMessage(topic, "Three-" + id).subscribe(() -> cdl.countDown());
                try {
                    cdl.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Sender sender1 = new Sender("1");
        Sender sender2 = new Sender("2");


        sender1.start();
        sender2.start();
        sender1.join();
        sender2.join();
        ProducerRecord[] producerRecords = kafkaObjectFactory.mockProducer().history().toArray(new ProducerRecord[0]);
        stream(producerRecords).forEach(pr -> assertThat(pr.topic(), is(topic)));
        stream(producerRecords).forEach(pr -> assertNull(pr.key()));
        assertTrue(stream(producerRecords).map(pr -> pr.value()).anyMatch(val -> val.equals("One-1")));
        assertTrue(stream(producerRecords).map(pr -> pr.value()).anyMatch(val -> val.equals("One-2")));
        assertTrue(stream(producerRecords).map(pr -> pr.value()).anyMatch(val -> val.equals("Two-1")));
        assertTrue(stream(producerRecords).map(pr -> pr.value()).anyMatch(val -> val.equals("Two-2")));
        assertTrue(stream(producerRecords).map(pr -> pr.value()).anyMatch(val -> val.equals("Three-1")));
        assertTrue(stream(producerRecords).map(pr -> pr.value()).anyMatch(val -> val.equals("Three-2")));
    }

    @Test
    void sendMessageWithException() throws Exception {
        sut = KafkaCommAdapter.<String>builder()
                .setMessageMarshaller(message -> { throw new MyException("4 test");})
                .setMessageUnmarshaller(message -> message)
                .setKafkaObjectFactory(kafkaObjectFactory)
                .setMetrics(new Metrics())
                .setIdentifier("Test")
                .build();

        TestObserver<Void> observer = sut.sendMessage("someTopic","Hallo").test();
        observer.await(1, TimeUnit.SECONDS);
        observer.assertError(MyException.class);
    }

    private class MyException extends RuntimeException {
        MyException(String message) {
            super(message);
        }
    }

}
