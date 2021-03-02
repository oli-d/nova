/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.websockets;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class EchoServer /*implements CommandLineRunner*/ {
//    private WebSocketAdapter webSocketAdapter;
//
//    public EchoServer (WebSocketAdapter webSocketAdapter) {
//        this.webSocketAdapter = webSocketAdapter;
//    }
//
//    public static void main(String[] args) {
//        SpringApplication.run(EchoServer.class, args);
//    }
//
//    @Override
//    public void run (String...args) throws Exception {
//        /**
//         * subscribe to connecting websockets
//         */
//        webSocketAdapter.acceptConnections("/echo").subscribe(
                 // Subscribe to incoming messages
//                socket -> {
//                    System.out.println("New connection established, starting to listen to messages...");
//                    socket.messages(String.class)
//                            .subscribe(
//                                incomingMessage -> {
                                     // Get the WebSocket that represents the connection to the sender
//                                    WebSocket webSocket = incomingMessage.metaData.details.webSocket;
                                     // and just send the message back to the sender
//                                    webSocket.send(incomingMessage.message);
//                                }
//                            );
//                }
//        );
//
//        /**
//         * Connect to the "server side" endpoint
//         */
//        WebSocket initiatingEndpoint = webSocketAdapter.connectTo("ws://127.0.0.1:10000/echo");
//
//        /**
//         * Subscribe to messages returned from the echo server
//         */
//        initiatingEndpoint.messages().subscribe(
//                incomingMessage -> {
//                    System.out.println("Echo server returned " + incomingMessage.message);
//                });
//
//        /**
//         * and send a few messages
//         */
//        initiatingEndpoint.send("One");
//        initiatingEndpoint.send("Two");
//        initiatingEndpoint.send("Three");
//
//        try {
//            TimeUnit.SECONDS.sleep(5);
//        } catch (InterruptedException e) {
             // Restore interrupted state...
//            Thread.currentThread().interrupt();
//        }
//
//        webSocketAdapter.shutdown().get();
//    }
}
