websockets
=========

Reactive abstractions over WebSockets.

## 1. What is this about?
This package provides the ```WebSocketAdapter``` class which can be used to 
send and retrieve messages via WebSockets.

## 2. WebSocketAdapter instantiation

You can instantiate a new ```WebSocketAdapter``` instance programmatically using its builder. The possible
configuration values you can pass are


  | Parameter                          | Description                                              | Default value |
  |------------------------------------|----------------------------------------------------------|---------------|
  | identifier                         | the identifier to assign to the HttpAdapter.             | <null> |
  | | | |
  | httpServer                         | the ```HttpServer``` instance, handling the incoming communication. | none, attribute is mandatory |
  | httpClient                         | the ```HttpClient``` instance, handling the outgoing communication. | none, attribute is mandatory |

# 3. Usage

The main class you will be using is ```WebSocketAdapter```. As with all adapters in the
nova communication landscape, this adapter provides the client and server part of the 
communication.  The terms "Client" and "Server" do not really fit very well to a 
bi-directional communication channel. What we refer to here is the ability to accept
new connections (that would be the "server" part) or initiate them ("client"). Once
the WebSocket has been created, both ends are equal peers.

Once you got hold of a ```WebSocketAdapter``` you can use it to create an ```Endpoint``` instance. You 
can do this in two ways

* If you want to wait for clients to connect to you, call ```WebSocketAdapter.acceptConnections()```. 

* If you want to connect to a server, invoke ```WebSocketAdapter.connectTo()```

Both methods require you to pass in the origin (either the one you want to connect to or the
one you are accepting clients on) and return an ```Endpoint``` instance that you can use to send and 
retrieve messages.

To see this in real life, here's how the implementation of an EchoServer would look like:

```Java
        // Instantiate the WebSocketAdapter
        WebSocketAdapter webSocketAdapter = webSocketAdapter();

        // subscribe to connecting websockets
        webSocketAdapter.acceptConnections("/echo").subscribe(
                // Subscribe to incoming messages
                socket -> {
                    System.out.println("New connection established, starting to listen to messages...");
                    socket.messages(String.class)
                            .subscribe(
                                incomingMessage -> {
                                    // Get the WebSocket that represents the connection to the sender
                                    WebSocket webSocket = incomingMessage.metaData.details.webSocket;
                                    // and just send the message back to the sender
                                    webSocket.send(incomingMessage.message);
                                }
                            );
                }
        );
```

The code to connect to that server and send a few messages would look like that:

```Java
        // Connect to the "server side" endpoint
        ClientEndpoint initiatingEndpoint = webSocketAdapter.connectTo("ws://127.0.0.1:10000/echo");
        // Subscribe to messages returned from the echo server
        initiatingEndpoint.messages(String.class).subscribe(
                incomingMessage -> {
                    System.out.println("Echo server returned " + incomingMessage.message);
                });
        // and send a few messages
        initiatingEndpoint.send("One");
        initiatingEndpoint.send("Two");
        initiatingEndpoint.send("Three");
```
_(You can find the full source code [here](./src/test/java/examples/EchoServer.java/README.md))_
