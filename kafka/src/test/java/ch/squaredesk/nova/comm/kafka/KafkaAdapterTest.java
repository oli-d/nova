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

import com.github.charithe.kafka.*;
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.TestObserver;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.common.utils.Sanitizer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(KafkaJunitExtension.class)
@KafkaJunitExtensionConfig(startupMode = StartupMode.WAIT_FOR_STARTUP)
class KafkaAdapterTest {
    private static Logger logger = LoggerFactory.getLogger(KafkaAdapterTest.class);

    private KafkaAdapter<String> sut;
    private AdminClient adminClient;


    @BeforeEach
    void setUp(KafkaHelper kafkaHelper) throws Exception {
        Properties adminClientProps = new Properties();
        adminClientProps.put("bootstrap.servers", "127.0.0.1:" + kafkaHelper.kafkaPort());
//        String clientId = "KafkaAdminClient-"+UUID.randomUUID();
//        String groupId = identifier == null ? "KafkaAdapter-ReadGroup" : identifier + "ReadGroup";
//        setPropertyIfNotPresent(consumerProperties, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddress);
//        setPropertyIfNotPresent(consumerProperties, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        setPropertyIfNotPresent(consumerProperties, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        setPropertyIfNotPresent(consumerProperties, ConsumerConfig.CLIENT_ID_CONFIG, clientId);
//        setPropertyIfNotPresent(consumerProperties, ConsumerConfig.GROUP_ID_CONFIG, groupId);
        adminClient = AdminClient.create(adminClientProps);

        sut = KafkaAdapter.builder(String.class)
                .setServerAddress("127.0.0.1:" + kafkaHelper.kafkaPort())
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

    void ensureTopicsExists(String...topics) throws Exception {
        CreateTopicsResult createResult = adminClient.createTopics(
                Arrays.stream(topics)
                        .map(topic -> new NewTopic(topic, 1, (short)1))
                        .collect(Collectors.toList())
        );
        // wait until all topics were created
        createResult.all().get();
    }

    @Test
    void resubscriptionWorks(KafkaHelper kafkaHelper) throws Exception {
        String topic = "topic4SubsTest";
        ensureTopicsExists(topic);
        // send two messages and assure they were received by the subscriber
        kafkaHelper.produceStrings(topic, "One", "Two"); // note: order not guaranteed

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


        cdl.await(60, SECONDS);
        assertThat(cdl.getCount(),is(0L));
        assertThat(counter.get(), is(2));
        assertThat(messages, containsInAnyOrder("One", "Two"));

        // dispose the subscription, resubscribe and send another message
        logger.error("Disposing subscription");
        subscription1.dispose();
        logger.error("subscription disposed :-)");

        CountDownLatch cdl2 = new CountDownLatch(1);
        List<String> messages2 = new ArrayList<>();
        logger.error("Subscribing again");
        Disposable subscription2 = sut.messages(topic).subscribe(x -> {
            messages2.add(x);
            cdl2.countDown();
        });
        logger.error("Subscribed again :-)");

        // ensure that only the second subscription was invoked
        kafkaHelper.produceStrings(topic, "Three");

        cdl2.await(10, SECONDS);
        assertThat(counter.get(), is(2));
        assertThat(cdl2.getCount(), is(0L));
        assertThat(messages, containsInAnyOrder("One", "Two"));
        assertThat(messages2, anyOf(contains("Three"), containsInAnyOrder("One", "Two", "Three")));

        subscription2.dispose();
    }

    @Test
    void multipleSubscribersSupportedOnSingleQueue() throws Exception {
        String topic = "multipleSubscriberTopic";
        ensureTopicsExists(topic);
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
    void multipleTopicsProperlySupported(KafkaHelper kafkaHelper) throws Exception {
        String topicEven = "even";
        String topicOdd = "odd";
        ensureTopicsExists(topicEven, topicOdd);

        CountDownLatch cdlEven = new CountDownLatch(4);
        CountDownLatch cdlOdd = new CountDownLatch(6);

        Consumer<String> messageConsumerOdd = msg -> cdlOdd.countDown();
        Consumer<String> messageConsumerEven = msg -> cdlEven.countDown();
        sut.messages(topicEven).subscribe(messageConsumerEven);
        sut.messages(topicEven).subscribe(messageConsumerEven);
        sut.messages(topicOdd).subscribe(messageConsumerOdd);
        sut.messages(topicOdd).subscribe(messageConsumerOdd);

        kafkaHelper.produceStrings(topicOdd, "1", "3", "5");
        kafkaHelper.produceStrings(topicEven, "2", "4");

        cdlEven.await(20, SECONDS);
        cdlOdd.await(20, SECONDS);

        assertThat(cdlEven.getCount(), is (0L));
        assertThat(cdlOdd.getCount(), is (0L));
    }

    @Test
    void errorInMessageHandlingKillsSubscription(KafkaHelper kafkaHelper) throws Exception {
        String topic = "topic4SubsErrorTest";
        ensureTopicsExists(topic);

        // send two good and one bad message
        KafkaProducer<String, String> stringProducer = kafkaHelper.createStringProducer();
        stringProducer.send(new ProducerRecord<>(topic, "1")).get();
        logger.error("Sent 1");
        stringProducer.send(new ProducerRecord<>(topic, "Two")).get();
        logger.error("Sent Two");
        stringProducer.send(new ProducerRecord<>(topic, "3")).get();
        logger.error("Sent 3");

        AtomicInteger counter = new AtomicInteger();
        List<Integer> messagesFromSut = new ArrayList<>();
        sut.messages(topic).subscribe(
                x -> {
                    logger.error("Received " + x);
                    try {
                        messagesFromSut.add(Integer.parseInt(x));
                    } finally {
                        counter.incrementAndGet();
                    }
                }
        );

        List<String> messagesFromBroker = kafkaHelper.consumeStrings(topic, 3).get(20, SECONDS);
        assertThat(messagesFromBroker, contains("1", "Two", "3"));
        // sleep a few seconds more to make sure no more messages wil be consumed
        SECONDS.sleep(10);
        assertThat(counter.get(),is(2));
        assertThat(messagesFromSut, containsInAnyOrder(1));
    }

    @Test
    void errorInMessageHandlingForOneSubscriptionDoesNotAffectOtherSubscriptions(KafkaHelper kafkaHelper) throws Exception {
        String topic = "topic4SubsErrorMultipleTest";
        ensureTopicsExists(topic);

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
        KafkaProducer<String, String> stringProducer = kafkaHelper.createStringProducer();
        stringProducer.send(new ProducerRecord<>(topic, "1")).get();
        stringProducer.send(new ProducerRecord<>(topic, "Two")).get();
        stringProducer.send(new ProducerRecord<>(topic, "3")).get();

        cdlGood.await(10, SECONDS);
        assertThat(cdlGood.getCount(),is(0L));
        assertThat(counterBrokenSubscriprion.get(),is(2));
        assertThat(messagesGood, containsInAnyOrder(1, 3));
    }

    @Test
    void messageMarshallingErrorOnSendForwardedToSubscriber(KafkaHelper kafkaHelper) throws Exception {
        sut = KafkaAdapter.builder(String.class)
                .setServerAddress("127.0.0.1:" + kafkaHelper.kafkaPort())
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
    void sendMessage(KafkaHelper kafkaHelper) throws Exception {
        String topic = "topicForSendTest";
        ensureTopicsExists(topic);

        sut.sendMessage(topic, "One").blockingAwait();
        sut.sendMessage(topic, "Two").blockingAwait();
        sut.sendMessage(topic, "Three").blockingAwait();

        List<String> messages = kafkaHelper.consumeStrings(topic, 3).get(10, SECONDS);
        assertThat(messages.size(), is(3));
        assertThat(messages, contains("One", "Two", "Three"));
    }

    @Test
    void sendingCanBeDoneFromMultipleThreads(KafkaHelper kafkaHelper) throws Exception {
        String topic = "topicForMultiThreadSendTest";
        ensureTopicsExists(topic);
        class Sender extends Thread {
            private final String id;

            public Sender(String id) {
                super("MultiThreadedSendTestSender-" + id);
                this.id = id;
            }

            @Override
            public void run() {
                sut.sendMessage(topic, "One-" + id).blockingAwait();
                sut.sendMessage(topic, "Two-" + id).blockingAwait();
                sut.sendMessage(topic, "Three-" + id).blockingAwait();
            }
        }
        Sender sender1 = new Sender("1");
        Sender sender2 = new Sender("2");

        sender1.start();
        sender2.start();
        sender1.join();
        sender2.join();

        List<String> messages = kafkaHelper.consumeStrings(topic, 6).get(20, SECONDS);
        assertThat(messages.size(), is(6));
        assertThat(messages, containsInAnyOrder("One-1", "Two-1", "Three-1", "One-2", "Two-2", "Three-2"));
    }
}
