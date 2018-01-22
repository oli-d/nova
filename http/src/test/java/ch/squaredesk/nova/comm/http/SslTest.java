package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

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
        sut = new RpcServer<>(httpServer, s->s, s->s, new Metrics());
        rpcClient = new RpcClient<>(null, client, s -> s, s -> s, new Metrics());
    }

    KeyStore readKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        java.io.FileInputStream fis = null;
        try {
            fis = new java.io.FileInputStream("src/test/resources/ssl/truststore.jks");
            ks.load(fis, "storepass".toCharArray());
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return ks;
    }

    @AfterEach
    void tearDown() {
        if (sut!=null) sut.shutdown();
    }

    @Test
    void requestsProperlyDispatched() throws Exception {
        sut.start();
        int numRequests = 5;
        String path = "/bla";
        CountDownLatch cdl = new CountDownLatch(numRequests);
        Flowable<RpcInvocation<String, String, HttpSpecificInfo>> requests = sut.requests(path);
        requests.subscribeOn(Schedulers.io()).subscribe(rpcInvocation -> {
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
                String urlAsString = "https://" + rsc.interfaceName + ":" + rsc.port + path + "?p=" + i;

                MessageSendingInfo<URL, HttpSpecificInfo> msi = new MessageSendingInfo.Builder<URL, HttpSpecificInfo>()
                        .withDestination(new URL(urlAsString))
                        .withTransportSpecificInfo(new HttpSpecificInfo(HttpRequestMethod.POST))
                        .build();

                rpcClient.sendRequest("{}", msi, 15, TimeUnit.SECONDS).blockingGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}