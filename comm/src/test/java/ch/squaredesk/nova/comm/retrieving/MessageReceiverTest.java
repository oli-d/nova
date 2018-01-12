/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.retrieving;

import ch.squaredesk.nova.comm.TestTransportInfo;

import java.util.List;
import java.util.function.Consumer;

class MessageReceiverTest {
    private List<String> destinationsSubscribedTo;
    private List<String> destinationsUnsubscribedFrom;
    private MessageReceiver<String, String, String, TestTransportInfo> sut;
    private List<Consumer<IncomingMessage<String, String, TestTransportInfo>>> incomingMessageConsumers;

/*
    @BeforeEach
    void setup() {
        destinationsSubscribedTo = new ArrayList<>();
        destinationsUnsubscribedFrom = new ArrayList<>();
        incomingMessageConsumers = new ArrayList<>();

        sut = new MessageReceiver<String, String, String, TestTransportInfo>(s -> s, new Metrics()) {
            @Override
            protected Observable<IncomingMessage<String, String, TestTransportInfo>> doSubscribe(String destination) {
                destinationsSubscribedTo.add(destination);
                return Observable.create(s -> {
                    incomingMessageConsumers.add(incomingMessageConsumers -> s.onNext(incomingMessageConsumers));
                });
            }

            @Override
            protected void doUnsubscribe(String destination) {
                destinationsUnsubscribedFrom.add(destination);
            }
        };
    }

    private void sendMessageToSut(String message) {
        IncomingMessageDetails imd = new IncomingMessageDetails.Builder<String, TestTransportInfo>()
                .withDestination(null)
                .withTransportSpecificDetails(null)
                .build();
        IncomingMessage im = new IncomingMessage(message, imd);
        incomingMessageConsumers.forEach(consumer -> consumer.accept(im));
    }

    @Test
    void instanceCannotBeCreatedWithoutUnmarshaller() {
        Throwable t = assertThrows(NullPointerException.class,
                () -> new MessageReceiver<Object, Object, Object, Object>(null, new Metrics()) {
                    @Override
                    protected Observable<IncomingMessage<Object, Object, Object>> doSubscribe(Object destination) {
                        return null;
                    }

                    @Override
                    protected void doUnsubscribe(Object destination) {
                    }
                });
        assertThat(t.getMessage(), containsString("messageUnmarshaller"));
    }

    @Test
    void exceptionInMessageReceivingForwardedToSubscriber() {
        sut = new MessageReceiver<String, String, String, TestTransportInfo>(s -> s, new Metrics()) {
            @Override
            protected Observable<IncomingMessage<String, String, TestTransportInfo>> doSubscribe(String destination) {
                destinationsSubscribedTo.add(destination);
                return Observable.create(s -> {
                    incomingMessageConsumers.add(incomingMessage -> {
                        s.onError(new RuntimeException("for test"));
                    });
                });
            }

            @Override
            protected void doUnsubscribe(String destination) {
            }
        };

        TestSubscriber<IncomingMessage<String, String, TestTransportInfo>> testSubscriber =
                sut.messages("dest", BackpressureStrategy.BUFFER).test();
        sendMessageToSut("bla"); // to trigger unmarshalling
        testSubscriber.assertError((Predicate<Throwable>) t -> t instanceof RuntimeException && t.getMessage().equals("for test"));
    }

    @Test
    void errorThrownIfSubscribingToNullDestination() {
        Throwable t = assertThrows(NullPointerException.class,
                () -> sut.messages(null, BackpressureStrategy.BUFFER));
        assertThat(t.getMessage(), containsStringIgnoringCase("destination"));

    }

    @Test
    void errorThrownIfSubscribingWithoutBackpressureStrategy() {
        Throwable t = assertThrows(NullPointerException.class,
                () -> sut.messages("des", null));
        assertThat(t.getMessage(), containsStringIgnoringCase("backpressureStrategy"));
    }

    @Test
    void connectionOnlyEstablishedForFirstSubscriber() {
        sut.messages("1", BackpressureStrategy.BUFFER).test();
        assertThat(destinationsSubscribedTo.size(), is(1));
        assertThat(destinationsSubscribedTo, contains("1"));

        sut.messages("1", BackpressureStrategy.BUFFER).test();
        assertThat(destinationsSubscribedTo.size(), is(1));

        sut.messages("2", BackpressureStrategy.BUFFER).test();
        assertThat(destinationsSubscribedTo.size(), is(2));
        assertThat(destinationsSubscribedTo, contains("1","2"));
        assertThat(destinationsUnsubscribedFrom.size(), is(0));

        sut.messages("1", BackpressureStrategy.BUFFER).test();
        assertThat(destinationsSubscribedTo.size(), is(2));
    }

    @Test
    void connectionDestroyedIfLastSubscriberIsDone() {
        TestSubscriber<IncomingMessage<String, String, TestTransportInfo>> testSubscriber1 = sut.messages("1", BackpressureStrategy.BUFFER).test();
        TestSubscriber<IncomingMessage<String, String, TestTransportInfo>> testSubscriber2 = sut.messages("1", BackpressureStrategy.BUFFER).test();
        TestSubscriber<IncomingMessage<String, String, TestTransportInfo>> testSubscriber3 = sut.messages("2", BackpressureStrategy.BUFFER).test();
        TestSubscriber<IncomingMessage<String, String, TestTransportInfo>> testSubscriber4 = sut.messages("2", BackpressureStrategy.BUFFER).test();

        assertThat(destinationsUnsubscribedFrom.size(), is(0));

        testSubscriber1.dispose();
        assertThat(destinationsUnsubscribedFrom.size(), is(0));

        testSubscriber2.dispose();
        assertThat(destinationsUnsubscribedFrom.size(), is(1));
        assertThat(destinationsUnsubscribedFrom, contains("1"));

        testSubscriber4.dispose();
        assertThat(destinationsUnsubscribedFrom.size(), is(1));

        testSubscriber3.dispose();
        assertThat(destinationsUnsubscribedFrom.size(), is(2));
        assertThat(destinationsUnsubscribedFrom, contains("1", "2"));
    }
*/
}
