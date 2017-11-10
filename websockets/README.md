# websockets

This artifact bundles all classes that enable you to do WebSocket communication according
to the Nova patterns.

The main class you will be using is ```WebSocketAdapter```. As with all adapters in the
nova communication landscape, this adapter provides the client and server part of the 
communication.  The terms "Client" and "Server" do not really fit very well to a 
bi-directional communication channel. What we refer to here is the ability to accept
new connections (that would be the "server" part) or initiate them ("client"). Once
the WebSocket has been created, both ends are equal peers.

```WebSocketAdapter```s are created using a builder. You have to pass

* ```metrics``` - The ```Metrics``` instance used to capture communication metrics. **Mandatory**

* ```messageMarshaller``` - The function used to transform your messages to the wire format (always ```String```)
before they are sent out. **Mandatory**

* ```messageUnmarshaller``` - The function used to transform incoming messages (```String```) to your internal 
representation. **Mandatory**

* ```httpServer``` - The ```HttpServer``` instance used to listen for incoming connections. Can be null, but
in that case, connections cannot be accepted. The adapter will throw a ```RuntimeException``` if
you try. Also, make sure that the passed server is NOT started before the ```WebSocketAdapter``` instance is
created. Otherwise the WebSocket feature cannot be properly initialized. Since we like to fail fast, the
system checks the current state of the passed server, and if it is already running also throws a ```RuntimeException```

* ```httpClient``` - The ```AsyncHttpClient``` instance used to initiate connections. Can be null, but
in that case, connections cannot be initiated. The adapter will throw a ```RuntimeException``` if
you try.

Once you got hold of a ```WebSocketAdapter``` you can use it to create an ```Endpoint``` instance. You 
can do this in two ways

* If you want to wait for clients to connect to you, call ```WebSocketAdapter.acceptConnections()```. 

* If you want to connect to a server, invoke ```WebSocketAdapter.connectTo()```

Both methods require you to pass in the destination (either the one you want to connect to or the
one you are accepting clients on) and return an ```Endpoint``` instance that you can use to send and 
retrieve messages.

To see this in real life, here's how the implementation of an EchoServer would look like:

```Java
        // Instantiate the WebSocketAdapter
        WebSocketAdapter<String> webSocketAdapter = webSocketAdapter();

        // Get the "server side" endpoint
        ServerEndpoint<String> acceptingEndpoint = webSocketAdapter.acceptConnections("/echo");
        // Subscribe to incoming messages
        acceptingEndpoint.messages(BackpressureStrategy.BUFFER).subscribe(
                incomingMessage -> {
                    // Get the WebSocket that represents the connection to the sender
                    WebSocket<String> webSocket = incomingMessage.details.transportSpecificDetails.webSocket;
                    // and just send the message back to the sender
                    webSocket.send(incomingMessage.message);
                });
```

The code to connect to that server and send a few messages would look like that:

```Java
        // Connect to the "server side" endpoint
        ClientEndpoint<String> initiatingEndpoint = webSocketAdapter.connectTo("ws://127.0.0.1:10000/echo");
        // Subscribe to messages returned from the echo server
        initiatingEndpoint.messages(BackpressureStrategy.BUFFER).subscribe(
                incomingMessage -> {
                    System.out.println("Echo server returned " + incomingMessage.message);
                });
        // and send a few messages
        initiatingEndpoint.send("One");
        initiatingEndpoint.send("Two");
        initiatingEndpoint.send("Three");
```
_(You can find the full source code [here](./src/test/java/examples/EchoServer.java/README.md))_
