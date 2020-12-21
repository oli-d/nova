/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.websockets.annotation.OnConnect;
import ch.squaredesk.nova.comm.websockets.annotation.OnMessage;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

public class EchoServerAnnotated implements CommandLineRunner {
    private WebSocketAdapter webSocketAdapter;

    public EchoServerAnnotated(WebSocketAdapter webSocketAdapter) {
        this.webSocketAdapter = webSocketAdapter;
    }

    public static void main(String[] args) {
        SpringApplication.run(EchoServer.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        /**
         * Connect to the "server side" endpoint
         */
        WebSocket initiatingEndpoint = webSocketAdapter.connectTo("ws://127.0.0.1:10000/echo");
        // Subscribe to messages returned from the echo server
        initiatingEndpoint.messages().subscribe(
                incomingMessage -> {
                    System.out.println("Echo server returned " + incomingMessage.message);
                });

        /**
         * and send a few messages
         */
        initiatingEndpoint.send("One");
        initiatingEndpoint.send("Two");
        initiatingEndpoint.send("Three");

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }

        webSocketAdapter.shutdown().get();
    }

    @Bean
    MyHandler myHandler() {
        return new MyHandler();
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
