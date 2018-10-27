package examples;

import ch.squaredesk.nova.comm.http.HttpAdapter;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.schedulers.Schedulers;
import org.glassfish.grizzly.http.server.HttpServer;

import java.io.IOException;
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

        // send a few messages and wait for the echo response
        int numMessages = 3;
        CountDownLatch cdl = new CountDownLatch(numMessages);
        for (int i = 0; i < numMessages; i++) {
            String message = "message #" + i;
            httpAdapter.sendPostRequest("http://127.0.0.1:10000/echo", message, String.class)
                    .subscribe(response -> {
                        System.out.println("Response for message >>" + message + "<< received: " + response);
                        cdl.countDown();
                    });
        }

        cdl.await();
    }
}
