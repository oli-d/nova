/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.observers.BaseTestConsumer;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

@Tag("medium")
class JmsAdapterIntegrationTest {
    private JmsAdapter<String> sut;
    private TestJmsHelper jmsHelper;
    private MyCorrelationIdGenerator myCorrelationIdGenerator = new MyCorrelationIdGenerator();

    private EmbeddedActiveMQBroker broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();
        if (!broker.brokerService.waitUntilStarted()) throw new RuntimeException("Unable to start embedded broker...");

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");

        Metrics metrics = new Metrics();
        sut = JmsAdapter.builder(String.class)
                .setConnectionFactory(connectionFactory)
                .setErrorReplyFactory(t -> "Error")
                .setCorrelationIdGenerator(myCorrelationIdGenerator)
                .setMessageMarshaller(s -> s)
                .setMessageUnmarshaller(s -> s)
                .setMetrics(metrics)
                .build();
        sut.start();

        jmsHelper = new TestJmsHelper(connectionFactory);
        jmsHelper.start();
    }

    @AfterEach
    void tearDown() {
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
    void rpcWorks() throws Exception {
        Destination queue = jmsHelper.echoOnQueue("myQueue1");

        TestObserver<RpcReply<String>> replyObserver = sut.sendRequest(queue, "aRequest", 500, MILLISECONDS).test().await();
        ch.squaredesk.nova.comm.rpc.RpcReply reply = replyObserver.values().get(0);
        assertThat(reply.result, is("aRequest"));
    }

    @Test
    void subscriptionAndRpcRepliesSupportedOnSameQueue() throws Exception {
        Destination queue = jmsHelper.createQueue("myQueue2");
        Destination sharedQueue = jmsHelper.createQueue("someDest");
        myCorrelationIdGenerator.delegate = () -> "correlationId";

        TestSubscriber<String> messageSubscriber = sut.messages(sharedQueue).test();
        TestObserver<RpcReply<String>> replyObserver = sut.sendRequest(queue, sharedQueue, "aRequest", null, 20, SECONDS).test();
        jmsHelper.sendReply(sharedQueue, "aReply1", null);
        jmsHelper.sendReply(sharedQueue, "aReply2", "correlationId");
        jmsHelper.sendReply(sharedQueue, "aReply3", null);

        replyObserver.await(21, SECONDS);
        replyObserver.assertComplete();
        replyObserver.assertValueCount(1);
        assertThat(replyObserver.values().get(0).result, is("aReply2"));

        messageSubscriber.awaitCount(2, BaseTestConsumer.TestWaitStrategy.SLEEP_1000MS, 5000);
        messageSubscriber.assertValueCount(2);
        assertThat(messageSubscriber.values(), contains("aReply1", "aReply3"));
    }

    private class MyCorrelationIdGenerator implements Supplier<String> {
        private Supplier<String> delegate;

        private MyCorrelationIdGenerator() {
            AtomicLong counter = new AtomicLong();
            this.delegate = () -> "c" + counter.incrementAndGet();
        }

        @Override
        public String get() {
            return delegate.get();
        }
    }

}
