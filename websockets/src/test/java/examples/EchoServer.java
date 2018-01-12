package examples;

import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.comm.websockets.WebSocketAdapter;
import ch.squaredesk.nova.comm.websockets.client.ClientEndpoint;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpoint;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.glassfish.grizzly.http.server.HttpServer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EchoServer {
    public static void main(String[] args) {
        new EchoServer().go();
    }

    private WebSocketAdapter<String> webSocketAdapter() {
        HttpServer httpServer = httpServer();

        WebSocketAdapter<String> webSocketAdapter = WebSocketAdapter.builder(String.class)
                .setHttpServer(httpServer)
                .setHttpClient(httpClient())
                .setMessageMarshaller(Object::toString)
                .setMessageUnmarshaller(String::valueOf)
                .setMetrics(metrics())
                .build();

        try {
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return webSocketAdapter;
    }

    private HttpServer httpServer() {
        return HttpServer.createSimpleServer("/", 10000);
    }

    private AsyncHttpClient httpClient() {
        AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
        return new AsyncHttpClient(cf);
    }

    private Metrics metrics() {
        return new Metrics();
    }

    private void go() {
        // Instantiate the WebSocketAdapter
        WebSocketAdapter<String> webSocketAdapter = webSocketAdapter();

        // Get the "server side" endpoint
        ServerEndpoint<String> acceptingEndpoint = webSocketAdapter.acceptConnections("/echo");
        // Subscribe to incoming messages
        acceptingEndpoint.messages()
                .subscribe(
                    incomingMessage -> {
                        // Get the WebSocket that represents the connection to the sender
                        WebSocket<String> webSocket = incomingMessage.details.transportSpecificDetails.webSocket;
                        // and just send the message back to the sender
                        webSocket.send(incomingMessage.message);
                    }
                );

        // Connect to the "server side" endpoint
        ClientEndpoint<String> initiatingEndpoint = webSocketAdapter.connectTo("ws://127.0.0.1:10000/echo");
        // Subscribe to messages returned from the echo server
        initiatingEndpoint.messages().subscribe(
                incomingMessage -> {
                    System.out.println("Echo server returned " + incomingMessage.message);
                });
        // and send a few messages
        initiatingEndpoint.send("One");
        initiatingEndpoint.send("Two");
        initiatingEndpoint.send("Three");

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
