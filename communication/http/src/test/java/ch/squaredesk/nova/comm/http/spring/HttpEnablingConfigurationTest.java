package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.comm.http.HttpAdapter;
import ch.squaredesk.nova.comm.http.RpcInvocation;
import ch.squaredesk.nova.comm.http.RpcReply;
import ch.squaredesk.nova.comm.http.SimpleHttpServer;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import com.sun.net.httpserver.HttpExchange;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.awaitility.Awaitility;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("medium")
class HttpEnablingConfigurationTest {
    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    private void setupContext(Class configClass) {
        ctx.register(configClass);
        ctx.refresh();
    }

    @AfterEach
    void shutdownServer() throws Exception  {
        HttpServer server = ctx.getBean(HttpServer.class);
        if (server!=null) {
            server.shutdown().get();
        }
    }

    @Test
    void importingConfigCreatesHttpAdapterWithDefaultSettings() {
        setupContext(DefaultConfig.class);

        HttpAdapter adapter = ctx.getBean(HttpAdapter.class);

        assertNotNull(adapter);
    }

    @Test
    void providedMessageTranscriberIsUsedForSending() throws Exception {
        setupContext(ConfigWithTranscriber.class);

        int port = HttpAdapter adapter = ctx.getBean(HttpAdapter.class);
        HttpAdapter adapter = ctx.getBean(HttpAdapter.class);

        Flowable<RpcInvocation<String>> requests = adapter.requests("/test", String.class);
        requests.subscribe(invocation -> {
            System.out.println("I got " + invocation.request.message);
            invocation.complete(invocation.request.message);
        });

        TestObserver<RpcReply<String>> testObserver = adapter.sendPostRequest("http://localhost:10000/test", 123, String.class).test();
        testObserver.assertValueCount(1);

        assertThat(testObserver.values().get(0).result, is("123"));
    }


    @Import({HttpEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class DefaultConfig {
    }

    @Import({HttpEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class ConfigWithTranscriber {
        @Bean
        public HttpServerStarter serverStarter() {
            return new HttpServerStarter();
        }
    }
}