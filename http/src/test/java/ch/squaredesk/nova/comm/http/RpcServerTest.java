package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpcServerTest {
    private HttpServerConfiguration rsc = new HttpServerConfiguration("localhost", 8888);
    private HttpServer httpServer = HttpServerFactory.serverFor(rsc);
    private RpcServer<String> sut;
    private RpcClient<String> restClient;
    private Logger logger = LoggerFactory.getLogger(RpcServerTest.class);


    @BeforeEach
    void setup() {
        sut = new RpcServer<>(httpServer, s->s, s->s, new Metrics());
        restClient = new RpcClient<>(null, s -> s, s -> s, new Metrics());
    }

    @AfterEach
    void tearDown() {
        if (sut!=null) sut.shutdown();
    }

    @Test
    void sutCannotBeCreatedWithoutConfigs() {
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> new RpcServer<String>(null, s->s, s-> s, new Metrics()));
        assertThat(npe.getMessage(), is("httpServer must not be null"));
    }

    @Test
    void sutCannotBeCreatedWithoutMarshaller() {
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> new RpcServer<>(httpServer, null, s-> s, new Metrics()));
        assertThat(npe.getMessage(), is("messageMarshaller must not be null"));
    }

    @Test
    void sutCannotBeCreatedWithoutUnmarshaller() {
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> new RpcServer<String>(httpServer, s-> s, null, new Metrics()));
        assertThat(npe.getMessage(), is("messageUnmarshaller must not be null"));
    }

    @Test
    void subscriptionsCanBeMadeAfterServerStarted() throws Exception {
        assertNotNull(sut.requests("/requests", BackpressureStrategy.BUFFER));
        sut.start();
        sut.requests("/failing", BackpressureStrategy.BUFFER);
    }

    @Test
    void requestsProperlyDispatched() throws Exception {
        sut.start();
        int numRequests = 5;
        String path = "/bla";
        CountDownLatch cdl = new CountDownLatch(numRequests);
        Flowable<RpcInvocation<String, String, HttpSpecificInfo>> requests = sut.requests(path, BackpressureStrategy.BUFFER);
        requests.observeOn(Schedulers.io()).subscribe(rpcInvocation -> {
            logger.info("I got my RPCI with p=" + rpcInvocation.transportSpecificInfo.parameters.get("p"));
            rpcInvocation.complete(" description " + rpcInvocation.transportSpecificInfo.parameters.get("p"));
            cdl.countDown();
        });

        IntStream.range(0,numRequests).forEach(i -> sendRestRequestInNewThread(path, i));

        cdl.await(20, TimeUnit.SECONDS);
        assertThat(cdl.getCount(), is (0L));
    }

    private void sendRestRequestInNewThread(String path, int i) {
        new Thread(() -> {
            try {
                String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path + "?p=" + i;

                MessageSendingInfo<URL, HttpSpecificInfo> msi = new MessageSendingInfo.Builder<URL, HttpSpecificInfo>()
                        .withDestination(new URL(urlAsString))
                        .withTransportSpecificInfo(new HttpSpecificInfo(HttpRequestMethod.POST))
                        .build();

                logger.info("Got back from server: " +
                        restClient.sendRequest("{}", msi, 15, TimeUnit.SECONDS).blockingGet()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}