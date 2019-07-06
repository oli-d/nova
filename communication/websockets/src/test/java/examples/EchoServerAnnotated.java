package examples;

import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.comm.websockets.WebSocketAdapter;
import ch.squaredesk.nova.comm.websockets.spring.WebSocketEnablingConfiguration;
import ch.squaredesk.nova.comm.websockets.spring.annotation.OnConnect;
import ch.squaredesk.nova.comm.websockets.spring.annotation.OnMessage;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

public class EchoServerAnnotated {
    public static void main(String[] args) throws Exception {
        new EchoServerAnnotated().go();
    }

    private void go() throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MyConfig.class);

        // get the WebSocketAdapter
        WebSocketAdapter webSocketAdapter = context.getBean(WebSocketAdapter.class);

        // Connect to the "server side" endpoint
        WebSocket initiatingEndpoint = webSocketAdapter.connectTo("ws://127.0.0.1:10000/echo");
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

    @Configuration
    @Import({WebSocketEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyConfig {
        @Bean
        MyHandler myHandler() {
            return new MyHandler();
        }
    }

    public static class MyHandler {
        @OnConnect("echo")
        public void onConnect(WebSocket webSocket) {

        }

        @OnMessage("echo")
        public void onMessage(String message, WebSocket webSocket) {
            webSocket.send(message);
        }
    }
}
