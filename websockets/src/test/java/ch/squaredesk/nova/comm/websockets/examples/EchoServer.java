package ch.squaredesk.nova.comm.websockets.examples;

import ch.squaredesk.nova.comm.websockets.Endpoint;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.comm.websockets.WebSocketAdapter;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import io.reactivex.BackpressureStrategy;
import org.glassfish.grizzly.http.server.HttpServer;

public class EchoServer {
    public static void main(String[] args) {
        new EchoServer().go();
    }

    private WebSocketAdapter<String> webSocketAdapter() {
        WebSocketAdapter<String> webSocketAdapter = WebSocketAdapter.<String>builder()
                .setMessageMarshaller(Object::toString)
                .setMessageUnmarshaller(String::valueOf)
                .setMetrics(metrics())
                .setHttpServer(httpServer())
                .setHttpClient(httpClient())
                .build();
    }

    private HttpServer httpServer() {

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
        Endpoint<String> endpoint = webSocketAdapter.acceptConnections("/echo");
        // Subscribe to incoming messages
        endpoint.messages(BackpressureStrategy.BUFFER).subscribe(
                incomingMessage -> {
                    // Get the WebSocket that represents the connection to the sender
                    WebSocket<String> webSocket = incomingMessage.details.transportSpecificDetails.webSocket;
                    // and just send the message back to the sender
                    webSocket.send(incomingMessage.message);
                });
    }
}
