package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@Tag("medium")
class SslTest {
    private HttpServerConfiguration rsc = HttpServerConfiguration.builder()
            .interfaceName("127.0.0.1")
            .port(10000)
            .sslKeyStorePath("src/test/resources/ssl/keystore.jks")
            .sslKeyStorePass("storepass") // also the keypass
            .build();
    private HttpServer httpServer = HttpServerFactory.serverFor(rsc);
    private RpcServer<String> sut;
    private RpcClient<String> rpcClient;
    private RequestSender requestSender;

    @BeforeEach
    void setup() throws Exception {
        KeyStore keyStore = readKeyStore();
        SSLContext sslContext = SSLContext.getInstance("SSL");
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "keystore_pass".toCharArray());
        sslContext.init(keyManagerFactory.getKeyManagers(),trustManagerFactory.getTrustManagers(), new SecureRandom());
        AsyncHttpClientConfig asyncHttpClientConfig = new AsyncHttpClientConfig.Builder().setSSLContext(sslContext).build();
        AsyncHttpClient client = new AsyncHttpClient(asyncHttpClientConfig);
        rpcClient = new RpcClient<>(null, client, s -> s, s -> s, new Metrics());
        requestSender = RequestSender.createFor("http://" + rsc.interfaceName + ":" + rsc.port, rpcClient);
        sut = new RpcServer<>(httpServer, s->s, s->s, new Metrics());
    }

    private KeyStore readKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        try (FileInputStream fis = new java.io.FileInputStream("src/test/resources/ssl/truststore.jks")) {
            ks.load(fis, "storepass".toCharArray());
        }
        return ks;
    }

    @AfterEach
    void tearDown() {
        if (sut!=null) sut.shutdown();
        if (rpcClient!=null) rpcClient.shutdown();
    }

    @Test
    void requestsProperlyDispatched() throws Exception {
        sut.start();
        ArrayList<String> parameterList = new ArrayList<>();
        int numRequests = 5;
        String path = "/bla";
        TestSubscriber<String> subscriber = sut.requests(path)
            .subscribeOn(Schedulers.io())
            .map(rpcInvocation -> rpcInvocation.request.metaData.details.parameters.get("p"))
            .test();

        Observable.range(0, numRequests)
            .subscribe(i -> requestSender.sendPostRestRequestInNewThread(path + "?p="+ i));

        await().atMost(20, SECONDS).until(subscriber::valueCount, is(numRequests));

        System.out.println("");
    }
}