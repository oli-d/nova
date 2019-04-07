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

import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("medium")
class KafkaAdapterErrorTest {
    private static final int KAFKA_PORT = 11_000;
    private KafkaAdapter sut;

    @BeforeEach
    void setUp() {
        sut = KafkaAdapter.builder()
                .setServerAddress("127.0.0.1:" + KAFKA_PORT)
                .setIdentifier("Test")
                .build();
    }

    @AfterEach
    void tearDown() {
        sut.shutdown();
    }

    @Test
    void subscribeWithNullDestinationEagerlyThrows() {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sut.messages(null, s -> s));
        assertThat(throwable.getMessage(), is("destination must not be null"));
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
        assertThat(throwable.getMessage(), startsWith("destination must not be null"));
    }

    @Test
    void sendMessageWithException() throws Exception {
        TestObserver<OutgoingMessageMetaData> observer = sut.sendMessage("someTopic","Hallo", message -> {
            throw new MyException("4 test");
        }).test();
        observer.await(1, TimeUnit.SECONDS);
        observer.assertError(MyException.class);
    }

    private class MyException extends RuntimeException {
        MyException(String message) {
            super(message);
        }
    }

}
