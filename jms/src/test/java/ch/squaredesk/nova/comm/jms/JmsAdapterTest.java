/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jms.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class JmsAdapterTest {
    private JmsAdapter<String> sut;
    private TestJmsHelper jmsHelper;
    private ConnectionFactory connectionFactory;

    private EmbeddedActiveMQBroker broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();
        if (!broker.brokerService.waitUntilStarted()) throw new RuntimeException("Unable to start embedded broker...");

        initializSut();
    }

    private void initializSut() throws Exception {
        initializSut(null);
    }
    private void initializSut(JmsRpcClient<String> rpcClient) throws Exception {
        connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");
        sut = JmsAdapter.builder(String.class)
                .setConnectionFactory(connectionFactory)
                .setRpcClient(rpcClient)
                .setErrorReplyFactory(error -> "Error")
                .build();
        sut.start();

        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            sut.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            jmsHelper.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            broker.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void instanceCannotBeCreatedWithoutConnectionFactory() {
        Throwable t = assertThrows(NullPointerException.class,
                () -> JmsAdapter.builder(String.class)
                .setRpcClient(null)
                .setErrorReplyFactory(error -> "Error")
                .build());
        assertThat(t.getMessage(),containsString("connectionFactory"));
    }

    @Test
    void instanceCannotBeCreatedWithoutErrorReplyFactory() {
        Throwable t = assertThrows(NullPointerException.class,
                () -> JmsAdapter.builder(String.class)
                        .setConnectionFactory(connectionFactory)
                        .setRpcClient(null)
                        .build());
        assertThat(t.getMessage(), containsString("errorReplyFactory"));
    }

    @Test
    void nullDestinationThrows() {
        assertThrows(NullPointerException.class,
                ()-> sut.sendRequest(null, "some request"));
    }

    @Test
    void rpcWorks() throws Exception {
        Destination queue = jmsHelper.echoOnQueue("myQueue1");

        TestObserver<String> replyObserver = sut.sendRequest(queue, "aRequest", 500, MILLISECONDS).test().await();
        replyObserver.assertValue("aRequest");
    }

    @Test
    void noReplyWithinTimeoutReturnsTimeoutException() throws Exception {
        Destination queue = jmsHelper.createQueue("myQueue2");
        TestObserver<String> replyObserver = sut.sendRequest(queue, "aRequest", 250, MILLISECONDS).test().await();
        replyObserver.assertError(TimeoutException.class);
    }

    @Test
    void testSubscription() throws Exception {
        // send two messages and assure they are received by the subsciber
        Destination queue = jmsHelper.createQueue("myQueue2");
        CountDownLatch[] cdlHolder = { new CountDownLatch(2) };
        List<String> messages = new ArrayList<>();
        Disposable subscription1 = sut.messages(queue).subscribe(x -> {
            messages.add(x);
            cdlHolder[0].countDown();
        });

        jmsHelper.sendMessage(queue, "One");
        jmsHelper.sendMessage(queue, "Two");
        cdlHolder[0].await(1, SECONDS);
        assertThat(cdlHolder[0].getCount(),is(0L));
        assertThat(messages, contains("One", "Two"));

        // dispose the subscriber, send another message and ensure that it was no longer invoked
        subscription1.dispose();
        CountDownLatch[] cdlHolder2 = { new CountDownLatch(1) };
        List<String> messages2 = new ArrayList<>();
        Disposable subscription2 = sut.messages(queue).subscribe(x -> {
            messages2.add(x);
            cdlHolder2[0].countDown();
        });

        jmsHelper.sendMessage(queue, "Three");

        cdlHolder2[0].await(5, SECONDS);
        assertThat(cdlHolder2[0].getCount(), is(0L));
        assertThat(messages, contains("One", "Two"));
        assertThat(messages2, contains("Three"));

        subscription2.dispose();
    }

    @Test
    void customHeadersGetTransported() throws Exception {
        Destination queue = jmsHelper.createQueue("headersQueue");
        MessageConsumer consumer = jmsHelper.createMessageConsumer(queue);

        Map<String, Object> sentHeaders = new HashMap<>();
        sentHeaders.put("k1","v1");
        sentHeaders.put("k2","v2");
        sut.sendMessage(queue, "some message", sentHeaders).test().await();

        Message receivedRawMessage = consumer.receive(5000);

        assertNotNull(receivedRawMessage);
        assertTrue(receivedRawMessage instanceof TextMessage);
        assertThat(receivedRawMessage.getObjectProperty("k1"),is("v1"));
        assertThat(receivedRawMessage.getObjectProperty("k2"),is("v2"));
    }

    @Test
    void subscribeWithNullDestinationThrows() throws Exception {
        assertThrows(NullPointerException.class, () -> sut.messages(null));
    }

    @Test
    void multipleSubscribersSupportedOnSingleQueue() throws Exception {
        Destination queue = jmsHelper.createQueue("multipleQueue");
        List<String> valuesSubscriber1, valuesSubscriber2, valuesSubscriber3;
        valuesSubscriber1 = new ArrayList<>();
        valuesSubscriber2 = new ArrayList<>();
        valuesSubscriber3 = new ArrayList<>();
        CountDownLatch[] cdlHolder = new CountDownLatch[3];
        cdlHolder[0] = new CountDownLatch(1);
        cdlHolder[1] = new CountDownLatch(1);
        sut.messages(queue).subscribe(x -> {
            valuesSubscriber1.add(x);
            cdlHolder[0].countDown();
        });
        sut.messages(queue).subscribe(x -> {
            valuesSubscriber2.add(x);
            cdlHolder[1].countDown();
        });

        jmsHelper.sendMessage(queue, "msg1");
        cdlHolder[0].await(1, SECONDS);
        assertThat(cdlHolder[0].getCount(),is(0L));
        cdlHolder[1].await(1, SECONDS);
        assertThat(cdlHolder[1].getCount(),is(0L));
        assertThat(valuesSubscriber1.size(),is(1));
        assertThat(valuesSubscriber1,contains("msg1"));
        assertThat(valuesSubscriber2.size(),is(1));
        assertThat(valuesSubscriber2,contains("msg1"));

        sut.messages(queue).subscribe(x -> {
            valuesSubscriber3.add(x);
            cdlHolder[2].countDown();
        });
        cdlHolder[0] = new CountDownLatch(1);
        cdlHolder[1] = new CountDownLatch(1);
        cdlHolder[2] = new CountDownLatch(1);
        jmsHelper.sendMessage(queue, "msg2");

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
    void subscriptionAndRpcRepliesSupportedOnSameQueue() throws Exception {
        Destination queue = jmsHelper.echoOnQueue("subsAndRpcQueue");
        Destination replyQueue = jmsHelper.createQueue("subsAndRpcReplyQueue");


        TestSubscriber<String> subscriber =
                sut.messages(replyQueue)
                        .take(1)
                        .test();

        String reply = sut.sendRequest(queue,replyQueue,"request",null,50, SECONDS)
                .blockingGet();
        assertThat(reply,is("request"));
        subscriber.assertEmpty();

        jmsHelper.sendMessage(replyQueue, "update");
        subscriber.await();
        subscriber.assertValues("update");
    }

    @Test
    void sendNullMessageReturnsError() throws JMSException {
        assertThrows(NullPointerException.class,
                () -> sut.sendMessage(jmsHelper.createQueue("not used"), null));
    }

    @Test
    void sendMessageOnNullQueueReturnsError() throws JMSException {
        assertThrows(NullPointerException.class,
                () -> sut.sendMessage(null, "message"));
    }

    @Test
    void sendMessage() throws Exception {
        Destination queue = jmsHelper.createQueue("sendTest");
        MessageConsumer consumer = jmsHelper.createMessageConsumer(queue);

        TestObserver<Void> observer = sut.sendMessage(queue,"Hallo").test();
        observer.await(1, TimeUnit.SECONDS);
        observer.assertComplete();

        Message message = consumer.receive(1000L);
        assertNotNull(message);
        assertTrue(message instanceof TextMessage);
        assertThat(((TextMessage)message).getText(),is("Hallo"));
    }

    @Test
    void sendMessageWithException() throws Exception {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");
        sut = JmsAdapter.builder(String.class)
                .setConnectionFactory(connectionFactory)
                .setErrorReplyFactory(error -> "Error")
                .setMessageMarshaller(message -> {
                    throw new MyException("4 test");
                })
                .build();
        sut.start();
        Destination queue = jmsHelper.createQueue("sendTest");

        TestObserver<Void> observer = sut.sendMessage(queue,"Hallo").test();
        observer.await(1, TimeUnit.SECONDS);
        observer.assertError(MyException.class);
    }

    @Test
    void destinationMustBeProvidedOnRpc() {
        assertThrows(NullPointerException.class,
                () -> sut.sendRequest(null, "message", null, 100L, MILLISECONDS));
    }

    @Test
    void defaultTimeoutUsedForRpcIfNoTimeUnitProvided() throws Exception {
        assertThrows(NullPointerException.class,
                () -> sut.sendRequest(jmsHelper.createTempQueue(), "message", 1777L, null));
    }

    @Test
    void defaultTimeoutUsedForRpcIfNoTimeoutProvided() throws Exception {
        MyRpcClient rpcClient = new MyRpcClient();
        initializSut(rpcClient);

        sut.sendRequest(jmsHelper.createTempQueue(), "message", null, null, null);

        assertNotNull(rpcClient.timeout);
        assertNotNull(rpcClient.timeUnit);
    }

    @Test
    void correlationIdGeneratedForRpcs() throws Exception {
        MyRpcClient rpcClient = new MyRpcClient();
        initializSut(rpcClient);

        sut.sendRequest(jmsHelper.createTempQueue(), "message", null, null, null);

        assertNotNull(rpcClient.messageSendingInfo.transportSpecificInfo.correlationId);
    }

    @Test
    void destinationAndMessagePassedUnchangedForRpc() throws Exception{
        MyRpcClient rpcClient = new MyRpcClient();
        initializSut(rpcClient);

        TemporaryQueue queue = jmsHelper.createTempQueue();
        String request = String.valueOf(UUID.randomUUID());
        sut.sendRequest(queue, request, null, null, null);

        assertThat(rpcClient.messageSendingInfo.destination, Matchers.sameInstance(queue));
        assertThat(rpcClient.request, Matchers.sameInstance(request));
    }

    private class MyException extends RuntimeException {
        public MyException(String message) {
            super(message);
        }

    }

    private class MyRpcClient extends JmsRpcClient<String> {
        private String request;
        private long timeout;
        private TimeUnit timeUnit;
        private MessageSendingInfo<Destination, JmsSpecificInfo> messageSendingInfo;

        public MyRpcClient() {
            super("TestRpcClient", null, null, new Metrics());
        }

        @Override
        public <RequestType extends String, ReplyType extends String> Single<ReplyType> sendRequest(
                RequestType request,
                MessageSendingInfo<Destination, JmsSpecificInfo> messageSendingInfo,
                long timeout, TimeUnit timeUnit) {
            this.request = request;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.messageSendingInfo = messageSendingInfo;
            return Single.never();
        }
    }
}
