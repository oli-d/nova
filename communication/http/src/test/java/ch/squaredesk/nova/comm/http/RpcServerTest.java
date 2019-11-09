package ch.squaredesk.nova.comm.http;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("medium")
class RpcServerTest {
    private HttpServerSettings rsc;
    private HttpServer httpServer;
    private RpcServer sut;

    @BeforeEach
    void setup() {
        rsc = HttpServerSettings.builder().interfaceName("localhost").port(PortFinder.findFreePort()).build();
        httpServer = HttpServerFactory.serverFor(rsc);
        sut = new RpcServer(httpServer, new Metrics());
    }

    @AfterEach
    void tearDown() {
        if (sut != null) sut.shutdown();
    }

    @Test
    void subscriptionsCanBeMadeAfterServerStarted() throws Exception {
        assertNotNull(sut.requests("/requests", String.class));
        sut.start();
        sut.requests("/failing", String.class);
    }

    @Test
    void requestsProperlyDispatched() throws Exception {
        sut.start();
        int numRequests = 15;
        String path = "/bla";
        List<String> responses = new ArrayList<>();

        sut.requests(path, String.class)
            .subscribe(rpcInvocation -> {
                rpcInvocation.complete(" description " + rpcInvocation.request.metaData.details.headers.get("p"));
            });

        IntStream.range(0, numRequests).forEach(i -> {
            String url = "http://" + rsc.interfaceName + ":" + rsc.port + path + "?p=" + i;
            responses.add(sendRestRequest(url, null).result);
        });

        await().atMost(20, TimeUnit.SECONDS).until(responses::size, is(numRequests));
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
    void largeRequestsProperlyDispatched() throws Exception {
        sut.start();
        String path = "/biiig";
        String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path;
        URL url = new URL(urlAsString);
        String hugeRequest = createStringOfLength(5 * 1024 * 1024);
        Flowable<RpcInvocation<String>> requests = sut.requests(path, String.class);
        requests.subscribeOn(Schedulers.io()).subscribe(rpcInvocation -> {
            rpcInvocation.complete(rpcInvocation.request.message);
        });

        HttpRequestSender.HttpResponse response = HttpRequestSender.sendPostRequest(url, hugeRequest);
        assertThat(response.replyMessage, is(hugeRequest));
    }

    @Test
    void largeResponseProperlyDispatched() throws Exception {
        sut.start();
        String path = "/biiigResponse";
        String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path;
        URL url = new URL(urlAsString);
        String hugeReply = createStringOfLength(15 * 1024 * 1024);
        Flowable<RpcInvocation<String>> requests = sut.requests(path, String.class);
        requests.subscribeOn(Schedulers.io()).subscribe(rpcInvocation -> {
            rpcInvocation.complete(hugeReply);
        });

        HttpRequestSender.HttpResponse response = HttpRequestSender.sendPostRequest(url, "request");
        assertThat(response.replyMessage, is(hugeReply));
    }

    @Test
    void httpReturnCodeCanBeSetWhenCompletingRpc() throws Exception {
        sut.start();
        String path = "/returnCodeTest";
        String urlAsString = "http://" + rsc.interfaceName + ":" + rsc.port + path;
        URL url = new URL(urlAsString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        Flowable<RpcInvocation<String>> requests = sut.requests(path, String.class);
        requests.subscribeOn(Schedulers.io()).subscribe(rpcInvocation -> {
            rpcInvocation.complete("someReply", 555);
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

        Flowable<RpcInvocation<String>> requests = sut.requests(path, String.class);
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
        Flowable<RpcInvocation<String>> requests = sut.requests(path, String.class);
        requests.subscribe(rpcInvocation -> rpcInvocation.completeExceptionally(new Exception("no content")));

        String url = "http://" + rsc.interfaceName + ":" + rsc.port + path + "?p=";
        RpcReply<String> reply = sendRestRequest(url, null);

        assertThat(reply.result, containsString("Internal server error"));
        assertThat(reply.metaData.details.statusCode, is(500));
    }

    private RpcReply<String> sendRestRequest(String path, String request) {
        try {
            URL url = new URL(path);
            HttpRequestSender.HttpResponse httpResponse = HttpRequestSender.sendPostRequest(url, null);
            return new RpcReply<>(httpResponse.replyMessage, new ReplyMessageMetaData(url, new ReplyInfo(httpResponse.returnCode, new HashMap<>())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}