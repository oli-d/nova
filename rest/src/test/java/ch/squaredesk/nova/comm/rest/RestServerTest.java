package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestServerTest {
    private RestServerConfiguration rsc = new RestServerConfiguration(8888, "localhost");
    private RestServer<String> sut;
    private RestClient<String> restClient;

    @BeforeEach
    void setup() {
        sut = new RestServer<>(rsc, new Metrics());
        restClient = new RestClient<>(null, s -> s, s -> s, new Metrics());
    }

    @AfterEach
    void tearDown() {
        if (sut!=null) sut.shutdown();
    }

    @Test
    void sutCannotBeCreatedWithoutConfigs() {
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> new RestServer<String>(null, new Metrics()));
        assertThat(npe.getMessage(), is("restServerConfiguration must not be null"));
    }

    @Test
    void subscriptionsCannotBeMadeAfterServerStarted() {
        assertNotNull(sut.requests("/requests", BackpressureStrategy.BUFFER));
        sut.start();
        assertThrows(IllegalStateException.class, () -> sut.requests("/failing", BackpressureStrategy.BUFFER));
    }

    @Test
    void requestsProperlyDispatched() throws Exception {
        int numRequests = 3;
        String path = "/bla";
        CountDownLatch cdl = new CountDownLatch(numRequests);
        Flowable<RpcInvocation<String, String, HttpSpecificInfo>> requests = sut.requests(path, BackpressureStrategy.BUFFER);
        requests.subscribe(rpcInvocation -> {
            rpcInvocation.complete(" description ");
            cdl.countDown();
        });
        sut.start();

        IntStream.range(0,numRequests).forEach(i -> sendRestRequestInNewThread(path));

        cdl.await(2000, TimeUnit.SECONDS);
        assertThat(cdl.getCount(), is (0L));
    }

    private void sendRestRequestInNewThread(String path) {
        new Thread(() -> {
            try {
                String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path;
                MessageSendingInfo<URL, HttpSpecificInfo> msi = new MessageSendingInfo.Builder<URL, HttpSpecificInfo>()
                        .withDestination(new URL(urlAsString))
                        .withTransportSpecificInfo(new HttpSpecificInfo(HttpRequestMethod.POST))
                        .build();

                System.out.println(
                        restClient.sendRequest("{}", msi, 5, TimeUnit.SECONDS).blockingGet()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}