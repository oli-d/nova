package examples;

import ch.squaredesk.nova.comm.http.HttpAdapter;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.schedulers.Schedulers;
import org.glassfish.grizzly.http.server.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

public class EchoServer {
    public static void main(String[] args) throws Exception {
        new EchoServer().go();
    }

    private HttpAdapter httpAdapter() {
        HttpServer httpServer = httpServer();

        HttpAdapter httpAdapter = HttpAdapter.builder()
                .setHttpServer(httpServer)
                .setMetrics(metrics())
                .build();

        try {
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return httpAdapter;
    }

    private HttpServer httpServer() {
        return HttpServer.createSimpleServer("/", 10000);
    }

    private Metrics metrics() {
        return new Metrics();
    }

    private void go() throws Exception {
        // Instantiate the HttpAdapter
        HttpAdapter httpAdapter = httpAdapter();

        // Subscribe to incoming echo requests
        httpAdapter.requests("/echo", String.class)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        incomingMessage -> {
                            // just send the message back to the sender
                            incomingMessage.complete(incomingMessage.request.message);
                        }
                );

        // send a few messages and cast the response to the format we want
        String destination = "http://127.0.0.1:10000/echo";
        CountDownLatch cdl = new CountDownLatch(3);
        httpAdapter
                .sendPostRequest(destination, "One", String.class)
                .subscribe(reply -> System.out.println("Received String response " + reply.result + " for message \"One\""));
        httpAdapter
                .sendPostRequest(destination, "2", Long.class)
                .subscribe(reply -> System.out.println("Received Long response " + reply.result + " for message \"2\""));
        httpAdapter
                .sendPostRequest(destination, "3.5", BigDecimal.class)
                .subscribe(reply -> System.out.println("Received BigDecimal response " + reply.result + " for message \"3.5\""));

    }
}
