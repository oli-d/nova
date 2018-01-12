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
import ch.squaredesk.nova.metrics.Metrics;
import com.github.charithe.kafka.EphemeralKafkaBroker;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class KafkaAdapterTest {
    private static final int KAFKA_PORT = 11_000;
    private static EphemeralKafkaBroker kafkaBroker;
    private KafkaAdapter<String> sut;

    @BeforeAll
    static void startKafkaBroker() throws Exception {
        kafkaBroker = EphemeralKafkaBroker.create(KAFKA_PORT);
        kafkaBroker.start().get();
    }

    @AfterAll
    static void shutdownKafkaBroker() throws Exception {
        kafkaBroker.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        sut = KafkaAdapter.builder(String.class)
                .setServerAddress("127.0.0.1:" + KAFKA_PORT)
                .setIdentifier("Test" + UUID.randomUUID())
                .addProducerProperty(ProducerConfig.BATCH_SIZE_CONFIG, "1")
                .addConsumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .addConsumerProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        sut.shutdown();
    }

    @Test
    void subscriptionWorks() throws Exception {
        String topic = "topic4SubsTest";
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch cdl = new CountDownLatch(2);
        List<String> messages = new ArrayList<>();
        Disposable subscription1 = sut.messages(topic).subscribe(
            x -> {
                messages.add(x);
                counter.incrementAndGet();
                cdl.countDown();
            }
        );

        // send two messages and assure they were received by the subscriber
        sut.sendMessage(topic, "One").blockingAwait();
        sut.sendMessage(topic, "Two").blockingAwait();

        cdl.await(30, SECONDS);
        assertThat(cdl.getCount(),is(0L));
        assertThat(counter.get(), is(2));
        assertThat(messages, containsInAnyOrder("One", "Two"));

        // dispose the subscription, resubscribe and send another message
        subscription1.dispose();

        CountDownLatch cdl2 = new CountDownLatch(1);
        List<String> messages2 = new ArrayList<>();
        Disposable subscription2 = sut.messages(topic).subscribe(x -> {
            messages2.add(x);
            cdl2.countDown();
        });

        // ensure that only the second subscription was invoked
        sut.sendMessage(topic, "Three").blockingAwait();

        cdl2.await(10, SECONDS);
        assertThat(counter.get(), is(2));
        assertThat(cdl2.getCount(), is(0L));
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
        CountDownLatch cdl1 = new CountDownLatch(1);
        CountDownLatch cdl2 = new CountDownLatch(1);
        sut.messages(topic).subscribe(x -> {
            valuesSubscriber1.add(x);
            cdl1.countDown();
        });
        sut.messages(topic).subscribe(x -> {
            valuesSubscriber2.add(x);
            cdl2.countDown();
        });

        sut.sendMessage(topic, "msg1").blockingAwait();
        cdl1.await(10, SECONDS);
        assertThat(cdl1.getCount(),is(0L));
        cdl2.await(10, SECONDS);
        assertThat(cdl2.getCount(),is(0L));
        assertThat(valuesSubscriber1.size(),is(1));
        assertThat(valuesSubscriber1,contains("msg1"));
        assertThat(valuesSubscriber2.size(),is(1));
        assertThat(valuesSubscriber2,contains("msg1"));

        CountDownLatch cdl3 = new CountDownLatch(1);
        sut.messages(topic).subscribe(x -> {
            valuesSubscriber3.add(x);
            cdl3.countDown();
        });
        sut.sendMessage(topic, "msg2").blockingAwait();

        cdl3.await(10, SECONDS);
        assertThat(cdl3.getCount(), is(0L));
        assertThat(valuesSubscriber1.size(), is(2));
        assertThat(valuesSubscriber1, contains("msg1", "msg2"));
        assertThat(valuesSubscriber2.size(), is(2));
        assertThat(valuesSubscriber2, contains("msg1", "msg2"));
        assertThat(valuesSubscriber3.size(), is(1));
        assertThat(valuesSubscriber3, contains("msg2"));
    }

    @Test
    void multipleTopicsProperlySupported() throws Exception {
        String topicEven = "even";
        String topicOdd = "odd";

        CountDownLatch cdlEven = new CountDownLatch(4);
        CountDownLatch cdlOdd = new CountDownLatch(6);

        Consumer<String> messageConsumerOdd = msg -> cdlOdd.countDown();
        Consumer<String> messageConsumerEven = msg -> cdlEven.countDown();
        sut.messages(topicEven).subscribe(messageConsumerEven);
        sut.messages(topicEven).subscribe(messageConsumerEven);
        sut.messages(topicOdd).subscribe(messageConsumerOdd);
        sut.messages(topicOdd).subscribe(messageConsumerOdd);

        sut.sendMessage(topicOdd, "1").blockingAwait();
        sut.sendMessage(topicEven, "2").blockingAwait();
        sut.sendMessage(topicOdd, "3").blockingAwait();
        sut.sendMessage(topicEven, "4").blockingAwait();
        sut.sendMessage(topicOdd, "5").blockingAwait();

        cdlEven.await(20, SECONDS);
        cdlOdd.await(20, SECONDS);

        assertThat(cdlEven.getCount(), is (0L));
        assertThat(cdlOdd.getCount(), is (0L));
    }

    @Test
    void errorInMessageHandlingKillsSubscription() throws InterruptedException {
        String topic = "topic4SubsErrorTest";
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

        // send two good and one bad message
        sut.sendMessage(topic, "1").blockingAwait();
        sut.sendMessage(topic, "Two").blockingAwait();
        sut.sendMessage(topic, "3").blockingAwait();

        cdl.await(10, SECONDS);
        assertThat(cdl.getCount(),is(1L));
        assertThat(messages, containsInAnyOrder(1));
    }

    @Test
    void errorInMessageHandlingForOneSubscriptionDoesNotAffectOtherSubscriptions() throws InterruptedException {
        String topic = "topic4SubsErrorMultipleTest";
        AtomicInteger counterBrokenSubscriprion = new AtomicInteger();
        CountDownLatch cdlGood = new CountDownLatch(3);
        List<Integer> messagesGood = new ArrayList<>();
        List<Integer> messagesBroken = new ArrayList<>();
        sut.messages(topic).subscribe(
                x -> {
                    try {
                        messagesBroken.add(Integer.parseInt(x));
                    } finally {
                        counterBrokenSubscriprion.incrementAndGet();
                    }
                }
        );
        sut.messages(topic).subscribe(
                x -> {
                    try {
                        messagesGood.add(Integer.parseInt(x));
                    } catch (Exception e) {
                        // noop
                    } finally {
                        cdlGood.countDown();
                    }
                }
        );

        // send two good and one bad message
        sut.sendMessage(topic, "1").blockingAwait();
        sut.sendMessage(topic, "Two").blockingAwait();
        sut.sendMessage(topic, "3").blockingAwait();

        cdlGood.await(10, SECONDS);
        assertThat(cdlGood.getCount(),is(0L));
        assertThat(counterBrokenSubscriprion.get(),is(2));
        assertThat(messagesGood, containsInAnyOrder(1, 3));
    }

    @Test
    void messageMarshallingErrorOnSendForwardedToSubscriber() throws Exception {
        sut = KafkaAdapter.builder(String.class)
                .setServerAddress("127.0.0.1:" + KAFKA_PORT)
                .setIdentifier("Test")
                .addProducerProperty(ProducerConfig.BATCH_SIZE_CONFIG, "1")
                .addConsumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .addConsumerProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                .setMessageMarshaller(s -> {
                    throw new RuntimeException("for test");
                })
                .build();

        Completable completable = sut.sendMessage("dest", "myMessage");
        TestObserver<Void> observer = completable.test();
        observer.await();
        observer.assertError(RuntimeException.class);
        observer.assertErrorMessage("for test");
    }

    @Test
    void sendMessage() throws Exception {
        String topic = "topicForSendTest";

        List<String> messages = new ArrayList<>();
        CountDownLatch cdl = new CountDownLatch(3);

        sut.messages(topic).subscribe(msg -> {
            messages.add(msg);
            cdl.countDown();
        });

        sut.sendMessage(topic, "One").blockingAwait();
        sut.sendMessage(topic, "Two").blockingAwait();
        sut.sendMessage(topic, "Three").blockingAwait();

        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(messages.size(), is(3));
        assertThat(messages.get(0), is("One"));
        assertThat(messages.get(1), is("Two"));
        assertThat(messages.get(2), is("Three"));
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
                sut.sendMessage(topic, "One-" + id).blockingAwait();
                sut.sendMessage(topic, "Two-" + id).blockingAwait();
                sut.sendMessage(topic, "Three-" + id).blockingAwait();
            }
        }
        Sender sender1 = new Sender("1");
        Sender sender2 = new Sender("2");

        List<String> messages = new ArrayList<>();
        CountDownLatch cdl = new CountDownLatch(6);

        sut.messages(topic).subscribe(msg -> {
            messages.add(msg);
            cdl.countDown();
        });

        sender1.start();
        sender2.start();
        sender1.join();
        sender2.join();

        cdl.await(20, SECONDS);
        assertThat(cdl.getCount(), is(0L));

        assertThat(messages, containsInAnyOrder("One-1", "Two-1", "Three-1", "One-2", "Two-2", "Three-2"));
    }

}
