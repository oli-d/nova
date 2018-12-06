package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
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

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@Tag("medium")
class RpcServerSslTest {
    private HttpServerConfiguration rsc = HttpServerConfiguration.builder()
            .interfaceName("127.0.0.1")
            .port(10000)
            .sslKeyStorePath("src/test/resources/ssl/keystore.jks")
            .sslKeyStorePass("storepass") // also the keypass
            .build();
    private HttpServer httpServer = HttpServerFactory.serverFor(rsc);
    private RpcServer sut;
    private RpcClient rpcClient;

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
        rpcClient = new RpcClient(null, client, new Metrics());
        sut = new RpcServer(httpServer, new DefaultMessageTranscriberForStringAsTransportType(), new Metrics());
    }

    private KeyStore readKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        try (FileInputStream fis = new FileInputStream("src/test/resources/ssl/truststore.jks")) {
            ks.load(fis, "storepass".toCharArray());
        }
        return ks;
    }

    @AfterEach
    void tearDown() {
        if (sut!=null) sut.shutdown();
        if (rpcClient!=null) rpcClient.shutdown();
    }

    public static void nuke() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            X509Certificate[] myTrustedAnchors = new X509Certificate[0];
                            return myTrustedAnchors;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
        } catch (Exception e) {
        }
    }

    @Test
    void requestsProperlyDispatched() throws Exception {
        nuke();
        sut.start();
        int numRequests = 5;
        String path = "/bla";
        TestSubscriber<String> subscriber = sut.requests(path, String.class)
                .subscribeOn(Schedulers.io())
                .map(rpcInvocation -> rpcInvocation.request.metaData.details.headers.get("p"))
                .test();

        Observable.range(0, numRequests)
            .subscribe(i -> sendRestRequestInNewThread(path, i));

        await().atMost(20, SECONDS).until(subscriber::valueCount, is(numRequests));
    }


    private void sendRestRequestInNewThread(String path, int i) {
        new Thread(() -> {
            try {
                String urlAsString = "https://" + rsc.interfaceName + ":" + rsc.port + path + "?p=" + i;

                RequestMessageMetaData meta = new RequestMessageMetaData(
                        new URL(urlAsString),
                        new RequestInfo(HttpRequestMethod.POST));

                rpcClient.sendRequest("{}", meta, s->s, s->s, 15, SECONDS).subscribe();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


}