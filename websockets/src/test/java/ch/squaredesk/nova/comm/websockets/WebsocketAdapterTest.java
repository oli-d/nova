package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.TestSubscriber;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class WebsocketAdapterTest {
    HttpServer httpServer = HttpServer.createSimpleServer("/", 7777);
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder()
            // .setProxyServer(new ProxyServer("127.0.0.1", 38080))
            .build();
    AsyncHttpClient httpClient = new AsyncHttpClient(cf);

    WebSocketAdapter<Integer> sut;

    @BeforeEach
    void setup() {
        sut = WebSocketAdapter.<Integer>builder()
                .setMessageMarshaller(Object::toString)
                .setMessageUnmarshaller(Integer::parseInt)
                .setMetrics(new Metrics())
                .setHttpServer(httpServer)
                .setHttpClient(httpClient)
                .build();
    }

    void bla() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);

        PublishSubject<Long> publishSubject = PublishSubject.create();
        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                    publishSubject.onNext(System.currentTimeMillis());
                    System.out.println("Published @ " + new Date());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        System.out.println("Starting @ " + new Date());

        Thread t2 = new Thread(() -> {
            System.out.println("2.) Going to sleep");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("2.) Woke up, subscribing");
            publishSubject.toFlowable(BackpressureStrategy.BUFFER).subscribe(l -> {
                System.out.println("2.) Received + " + new Date(l) + " @ " + new Date());
            });
        });

        new Thread(() -> {
            System.out.println("1.) Going to sleep");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            t2.start();
            System.out.println("1.) Woke up, subscribing");
            publishSubject.toFlowable(BackpressureStrategy.BUFFER).subscribe(l -> {
                System.out.println("1.) Received + " + new Date(l) + " @ " + new Date());
            });

        }).start();

        cdl.await();
    }

    @Test
    void sendAndReceiveAfterInitiatingConnection() throws Exception {
        CountDownLatch cdl = new CountDownLatch(3);

        String destinationUri = "ws://echo.websocket.org/";

        Endpoint<Integer> endpoint = sut.connectTo(destinationUri);
        testEcho(destinationUri, endpoint);
    }

    /*
    @Test
    void sendAndReceiveAfterAcceptingConnection() throws Exception {
        String destinationUri = "echo";
        ClientEndpoint<Integer> clientEndpointAccepting = sut.acceptConnection(destinationUri);
        ClientEndpoint<Integer> clientEndpointInitiating = sut.connectToEndpoint("ws://127.0.0.1:7777/" + destinationUri);

        testSendAndReceiveOn("ws://127.0.0.1:7777/" +destinationUri, clientEndpointInitiating);
    }
*/

    void testEcho(String destination, Endpoint<Integer> endpoint) throws Exception {
        Flowable<IncomingMessage<Integer, String, WebSocketSpecificDetails>> messages = endpoint.messages();
        TestSubscriber<IncomingMessage<Integer, String, WebSocketSpecificDetails>> testSubscriber = messages.test();

        endpoint.send(1);
        endpoint.send(2);
        endpoint.send(33);

        long maxWaitTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20);
        while (testSubscriber.valueCount() < 3 && System.currentTimeMillis() < maxWaitTime) {
            TimeUnit.MILLISECONDS.sleep(500);
        }
        testSubscriber.assertValueCount(3);
        assertThat(testSubscriber.values().get(0).message, is(1));
        assertThat(testSubscriber.values().get(1).message, is(2));
        assertThat(testSubscriber.values().get(2).message, is(33));
        for (int i = 0; i < 3; i++) {
            assertNotNull(testSubscriber.values().get(i).details);
            assertThat(testSubscriber.values().get(i).details.destination, is(destination));
            assertNotNull(testSubscriber.values().get(i).details.transportSpecificDetails);
            assertThat(testSubscriber.values().get(i).details.transportSpecificDetails.session, is(clientEndpoint.session.blockingGet()));
        }
        testSubscriber.dispose();
        clientEndpoint.close();
    }
}