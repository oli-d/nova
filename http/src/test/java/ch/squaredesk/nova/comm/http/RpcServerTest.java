package ch.squaredesk.nova.comm.http;

import ch.squaredesk.net.HttpRequestSender;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("medium")
class RpcServerTest {
    private HttpServerConfiguration rsc = HttpServerConfiguration.builder().interfaceName("localhost").port(10000).build();
    private HttpServer httpServer = HttpServerFactory.serverFor(rsc);
    private RpcServer<String> sut;
    private RpcClient<String> rpcClient;

    @BeforeEach
    void setup() {
        sut = new RpcServer<>(httpServer, s -> s, s -> s, new Metrics());
        rpcClient = new RpcClient<>(null, new AsyncHttpClient(), s -> s, s -> s, new Metrics());
    }

    @AfterEach
    void tearDown() {
        if (sut != null) sut.shutdown();
        if (rpcClient != null) rpcClient.shutdown();
    }

    @Test
    void sutCannotBeCreatedWithoutConfigs() {
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> new RpcServer<>(null, s -> s, s -> s, new Metrics()));
        assertThat(npe.getMessage(), is("httpServer must not be null"));
    }

    @Test
    void sutCannotBeCreatedWithoutMarshaller() {
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> new RpcServer<>(httpServer, null, s -> s, new Metrics()));
        assertThat(npe.getMessage(), is("messageMarshaller must not be null"));
    }

    @Test
    void sutCannotBeCreatedWithoutUnmarshaller() {
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> new RpcServer<String>(httpServer, s -> s, null, new Metrics()));
        assertThat(npe.getMessage(), is("messageUnmarshaller must not be null"));
    }

    @Test
    void subscriptionsCanBeMadeAfterServerStarted() throws Exception {
        assertNotNull(sut.requests("/requests"));
        sut.start();
        sut.requests("/failing");
    }

    @Test
    void requestsProperlyDispatched() throws Exception {
        sut.start();
        int numRequests = 15;
        String path = "/bla";
        CountDownLatch cdl = new CountDownLatch(numRequests);
        sut.requests(path)
            .subscribeOn(Schedulers.io())
            .map(rpcInvocation -> {
                rpcInvocation.complete(" description " + rpcInvocation.request.metaData.details.headerParams.get("p"));
                cdl.countDown();
                return 1;
            }).test();

        IntStream.range(0, numRequests).forEach(i -> sendRestRequest(path, i));

        cdl.await(20, TimeUnit.SECONDS);
        assertThat(cdl.getCount(), is(0L));
    }

    private String createStringOfLength(int length) {
        switch (length) {
            case 0:
                return "";
            case 1:
                return "X";
            case 2:
                return "XX";
            default:
                char[] charArray = new char[Math.abs(length)];
                for (int i = 0; i < charArray.length; i++) {
                    charArray[i] = (i == 0 || i == charArray.length - 1 ? 'X' : '-');
                }
                return new String(charArray);
        }
    }

    @Test
    void largeRquestsProperlyDispatched() throws Exception {
        sut.start();
        String path = "/biiig";
        String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path;
        URL url = new URL(urlAsString);
        String hugeRequest = createStringOfLength(5 * 1024 * 1024);
        Flowable<RpcInvocation<String>> requests = sut.requests(path);
        requests.subscribeOn(Schedulers.io()).subscribe(rpcInvocation -> {
            rpcInvocation.complete(rpcInvocation.request.message);
        });

        HttpRequestSender.HttpResponse response = HttpRequestSender.sendPostRequest(url, hugeRequest);
        assertThat(response.replyMessage, is(hugeRequest));
    }

    @Test
    void httpReturnCodeCanBeSetWhenCompletingRpc() throws Exception {
        sut.start();
        String path = "/returnCodeTest";
        String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path;
        URL url = new URL(urlAsString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        Flowable<RpcInvocation<String>> requests = sut.requests(path);
        requests.subscribeOn(Schedulers.io()).subscribe(rpcInvocation -> {
            rpcInvocation.complete(555, "someReply");
        });

        connection.connect();
        assertThat(connection.getResponseCode(), is(555));
    }

    @Test
    void httpHeadersCanBeSetWhenCompletingRpc() throws Exception {
        sut.start();
        String path = "/returnCodeTest";
        String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path;
        URL url = new URL(urlAsString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Map<String,String> customHeaders = new HashMap<>();
        customHeaders.put("myParam", "myValue");

        Flowable<RpcInvocation<String>> requests = sut.requests(path);
        requests.subscribeOn(Schedulers.io()).subscribe(rpcInvocation -> {
            rpcInvocation.complete("someReply", customHeaders);
        });

        connection.connect();
        assertThat(connection.getHeaderField("myParam"), is("myValue"));
    }

    @Test
    void requestProcessingFailsOnServerSideAndErrorIsDispatched() throws Exception {
        sut.start();
        String path = "/fail";
        Flowable<RpcInvocation<String>> requests = sut.requests(path);
        requests.subscribe(rpcInvocation -> rpcInvocation.completeExceptionally(new Exception("no content")));

        String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path + "?p=";
        RequestMessageMetaData meta = new RequestMessageMetaData(
                new URL(urlAsString),
                new RequestInfo(HttpRequestMethod.POST));

        RpcReply<String> reply = rpcClient.sendRequest("{}", meta, 15, TimeUnit.SECONDS).blockingGet();
        assertThat(reply.result, containsString("Internal server error"));
        assertThat(reply.metaData.details.statusCode, is(500));
    }

    private void sendRestRequest(String path, int i) {
        try {
            String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path + "?p=" + i;
            System.out.println(">>>>> " + urlAsString);

            RequestMessageMetaData meta = new RequestMessageMetaData(
                    new URL(urlAsString),
                    new RequestInfo(HttpRequestMethod.POST));

            rpcClient.sendRequest("{}", meta, 15, TimeUnit.SECONDS).blockingGet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}