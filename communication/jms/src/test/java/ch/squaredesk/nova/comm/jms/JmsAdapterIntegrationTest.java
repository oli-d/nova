/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.rpc.RpcReply;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

@Tag("medium")
class JmsAdapterIntegrationTest {
    private JmsAdapter sut;
    private TestJmsHelper jmsHelper;
    private MyCorrelationIdGenerator myCorrelationIdGenerator = new MyCorrelationIdGenerator();

    private EmbeddedActiveMQBroker broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new EmbeddedActiveMQBroker();
        broker.start();
        if (!broker.brokerService.waitUntilStarted()) throw new RuntimeException("Unable to start embedded broker...");

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://embedded-broker?create=false");

        sut = JmsAdapter.builder()
                .setConnectionFactory(connectionFactory)
                .setCorrelationIdGenerator(myCorrelationIdGenerator)
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

        TestObserver<RpcReply<String, IncomingMessageMetaData<Destination, RetrieveInfo>>> replyObserver =
                sut.sendRequest(queue, "aRequest", String.class, Duration.ofMillis(500)).test().await();
        RpcReply reply = replyObserver.values().get(0);
        assertThat(reply.result(), is("aRequest"));
    }

    @Test
    void subscriptionAndRpcRepliesSupportedOnSameQueue() throws Exception {
        Destination queue = jmsHelper.createQueue("myQueue2");
        Destination sharedQueue = jmsHelper.createQueue("someDest");
        myCorrelationIdGenerator.delegate = () -> "correlationId";

        TestSubscriber<String> messageSubscriber = sut.messages(sharedQueue).test();
        TestObserver<RpcReply<String, IncomingMessageMetaData<Destination, RetrieveInfo>>> replyObserver =
                sut.sendRequest(queue, sharedQueue, "aRequest", null, String.class, Duration.ofSeconds(20)).test();
        jmsHelper.sendReply(sharedQueue, "aReply1", null);
        jmsHelper.sendReply(sharedQueue, "aReply2", "correlationId");
        jmsHelper.sendReply(sharedQueue, "aReply3", null);

        replyObserver.await(21, SECONDS);
        replyObserver.assertComplete();
        replyObserver.assertValueCount(1);
        assertThat(replyObserver.values().get(0).result(), is("aReply2"));

        messageSubscriber.awaitCount(2);
        messageSubscriber.assertValueCount(2);
        assertThat(messageSubscriber.values(), contains("aReply1", "aReply3"));
    }

    private static class MyCorrelationIdGenerator implements Supplier<String> {
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
