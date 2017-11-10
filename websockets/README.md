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
you try.

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
  WebSocketAdapter<String> webSocketAdapter = WebSocketAdapter.<String>builder()
                              .setMessageMarshaller(Object::toString)
                              .setMessageUnmarshaller(String::valueOf)
                              .setMetrics(metrics)
                              .setHttpServer(httpServer)
                              .setHttpClient(httpClient)
                              .build();

  // Get the "server side" endpoint
  Endpoint<String> endpoint = webSocketAdapter.acceptConnections("/echo");
  // Subscribe to incoming messages
  endpoint.messages(BackpressureStrategy.BUFFER).subscribe(
      incomingMessage -> {
          // Get the WebSocket that represents the connection to the sender
          WebSocket<String> webSocket = incomingMessage.details.transportSpecificDetails.webSocket; 
          // and just send the message back to the sender
          webSocket.send(incomingMessage.message));
  });
```

The code to connect to that server and send a few messages would look like that:

```Java
  // Instantiate the WebSocketAdapter
  WebSocketAdapter<String> webSocketAdapter = ...

  // Get the "client side" endpoint
  Endpoint<String> endpoint = webSocketAdapter.connectTo("ws://127.0.0.1/echo");
  // Subscribe to incoming replies
  endpoint.messages(BackpressureStrategy.BUFFER).subscribe(
      incomingMessage -> {
          // Simply print them out
          System.out.println("Echo server replied: " + incomingMessage.message));
  });
  // Send a message
  endpoint.send("Hello echo");
```

The ```WebSocket```s handle the real communication 