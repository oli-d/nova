package ch.squaredesk.nova.comm.websockets;

class ClientEndpointTest {
    /*
    ClientEndpoint<Integer> sut;
    Metrics metrics = new Metrics();

    @BeforeEach
    void setup() {
        sut = new ClientEndpoint<>("dest", Object::toString, Integer::parseInt, new MetricsCollector(metrics));
    }

    void main() throws Exception {
        String[] stringArray = new String[1];
        CountDownLatch cdl = new CountDownLatch(1);
        CountDownLatch cdlIntermediate = new CountDownLatch(1);
        CountDownLatch cdlFinish = new CountDownLatch(1);

        new Thread(() -> {
            // System.out.println("Enter something");
            try {
                sleep(2000);
                stringArray[0] = "new BufferedReader(new InputStreamReader(System.in)).readLine()";
                cdl.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Single<String> single = Single.fromCallable(() -> {
            cdl.await();
            return stringArray[0];
        });

        System.out.println("1) subs");
        single.subscribe(s -> {
            System.out.println("1) Got " + s);
            cdlIntermediate.countDown();
        });

        cdlIntermediate.await();
        System.out.println("2) subs");
        single.subscribe(s -> {
            System.out.println("2) Got " + s);
            cdlFinish.countDown();
        });

        cdlFinish.await();
    }

    @Test
    void metricsProperlyCaptured() throws Exception {
        Meter totalReceived = metrics.getMeter("websocket", "received", "total");
        Meter specificReceived = metrics.getMeter("websocket", "received", "dest");
        Meter totalUnparsableReceived = metrics.getMeter("websocket", "received", "unparsable", "total");
        Meter specificUnparsableReceived = metrics.getMeter("websocket", "received", "unparsable", "dest");
        Meter totalSent = metrics.getMeter("websocket", "sent", "total");
        Meter specificSent = metrics.getMeter("websocket", "sent", "dest");
        Counter totalSubscriptions = metrics.getCounter("websocket", "subscriptions", "total");

        assertThat(totalReceived.getCount(), is (0l));
        assertThat(specificReceived.getCount(), is (0l));
        assertThat(totalSent.getCount(), is (0l));
        assertThat(totalSubscriptions.getCount(), is (0l));
        assertThat(totalUnparsableReceived.getCount(), is(0l));
        assertThat(specificUnparsableReceived.getCount(), is(0l));

        DummySession session = new DummySession();
        sut.opened(session, null);
        assertThat(totalReceived.getCount(), is (0l));
        assertThat(specificReceived.getCount(), is(0l));
        assertThat(totalSent.getCount(), is (0l));
        assertThat(totalSubscriptions.getCount(), is (1l));
        assertThat(totalUnparsableReceived.getCount(), is(0l));
        assertThat(specificUnparsableReceived.getCount(), is(0l));

        sut.send(1);
        sut.send(2);
        assertThat(totalReceived.getCount(), is(0l));
        assertThat(specificReceived.getCount(), is(0l));
        assertThat(totalSent.getCount(), is(2l));
        assertThat(specificSent.getCount(), is(2l));
        assertThat(totalSubscriptions.getCount(), is(1l));
        assertThat(totalUnparsableReceived.getCount(), is(0l));
        assertThat(specificUnparsableReceived.getCount(), is(0l));

        sut.messages().subscribe();
        ((MessageHandler.Whole<String>) session.getMessageHandlers().iterator().next()).onMessage("1");
        ((MessageHandler.Whole<String>) session.getMessageHandlers().iterator().next()).onMessage("2");
        assertThat(totalReceived.getCount(), is(2l));
        assertThat(specificReceived.getCount(), is(2l));
        assertThat(totalSent.getCount(), is(2l));
        assertThat(specificSent.getCount(), is(2l));
        assertThat(totalSubscriptions.getCount(), is(1l));
        assertThat(totalUnparsableReceived.getCount(), is(0l));
        assertThat(specificUnparsableReceived.getCount(), is(0l));

        ((MessageHandler.Whole<String>) session.getMessageHandlers().iterator().next()).onMessage("a");
        ((MessageHandler.Whole<String>) session.getMessageHandlers().iterator().next()).onMessage("b");
        assertThat(totalReceived.getCount(), is(2l));
        assertThat(specificReceived.getCount(), is(2l));
        assertThat(totalSent.getCount(), is(2l));
        assertThat(specificSent.getCount(), is(2l));
        assertThat(totalSubscriptions.getCount(), is(1l));
        assertThat(totalUnparsableReceived.getCount(), is(2l));
        assertThat(specificUnparsableReceived.getCount(), is(2l));

        sut.close();
        assertThat(totalReceived.getCount(), is(2l));
        assertThat(specificReceived.getCount(), is(2l));
        assertThat(totalSent.getCount(), is(2l));
        assertThat(specificSent.getCount(), is(2l));
        assertThat(totalSubscriptions.getCount(), is(0l));
        assertThat(totalUnparsableReceived.getCount(), is(2l));
        assertThat(specificUnparsableReceived.getCount(), is(2l));
    }

    @Test
    void sendAttemptThrowsIfNoConnectionEstablishedYet() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            sut.send(1);
        });

        // simulate connection
        sut.opened(new DummySession(), null);

        // now the sending should be ok
        sut.send(2);
    }

    @Test
    void requestingMessagesThrowsIfNoConnectionEstablishedYet()  {
        assertThrows(IllegalStateException.class, () -> {
            sut.messages();
        });

        // simulate connection
        sut.opened(new DummySession(), null);

        // now the call should be ok
        assertNotNull(sut.messages());
    }

    @Test
    void openHandlerInvokedOnEvent() {
        AtomicInteger counter = new AtomicInteger();
        sut.onOpen(() -> counter.incrementAndGet());
        assertThat(counter.get(), is(0));

        sut.opened(new DummySession(), null);

        assertThat(counter.get(), is(1));
    }

    @Test
    void openHandlerInvokedEvenIfAnotherOneThrows() {
        sut.onOpen(() -> { throw new RuntimeException("I am the bad guy"); });
        AtomicInteger counter = new AtomicInteger();
        sut.onOpen(() -> counter.incrementAndGet());
        assertThat(counter.get(), is(0));

        sut.opened(new DummySession(), null);

        assertThat(counter.get(), is(1));
    }

    @Test
    void openHandlerInvokedIfRegisteredAfterConnectionAlreadyEstablished() {
        sut.opened(new DummySession(), null);
        AtomicInteger counter = new AtomicInteger();

        sut.onOpen(() -> counter.incrementAndGet());

        assertThat(counter.get(), is(1));
    }

    @Test
    void closedHandlerInvokedOnEventEvenIfAnotherOneThrows() {
        // bad guy
        sut.onClose(reason -> {
            throw new RuntimeException("I am the bad guy");
        });
        // good guy
        AtomicInteger counter = new AtomicInteger();
        sut.onClose(reason -> counter.incrementAndGet());

        sut.closed(new DummySession(), new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE,""));

        assertThat(counter.get(), is(1));
    }

    @Test
    void closedHandlerInvokedOnEvent() {
        AtomicInteger counter = new AtomicInteger();
        sut.onClose(reason -> counter.incrementAndGet());

        sut.closed(new DummySession(), new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE,""));

        assertThat(counter.get(), is(1));
    }

    @Test
    void errorHandlerInvokedOnEvent() {
        AtomicInteger counter = new AtomicInteger();
        sut.onError(reason -> counter.incrementAndGet());

        sut.errored(new DummySession(), new Throwable("for test"));

        assertThat(counter.get(), is(1));
    }

    @Test
    void errorHandlerInvokedOnEventEvenIfAnotherOneThrows() {
        // bad handler
        sut.onError(thr -> {
            throw new RuntimeException("I am the bad guy");
        });
        // good handler
        AtomicInteger counter = new AtomicInteger();
        sut.onError(reason -> counter.incrementAndGet());

        sut.errored(new DummySession(), new Throwable("for test"));

        assertThat(counter.get(), is(1));
    }
*/
}