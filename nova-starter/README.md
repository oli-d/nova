websockets
=========

Reactive abstractions over WebSockets.

## 1. What is this about?
This package provides the ```WebSocketAdapter``` class which can be used to 
send and retrieve messages via WebSockets.

## 2. WebSocketAdapter instantiation

The easiest way to retrieve a ```WebSocketAdapter``` instance is using Spring. For this, we provide
the class ```WebSocketEnablingConfiguration```. Simply import this in your own Spring configuration
and you can get a bean called "webSocketAdapter" from the ApplicationContext.

When you do so, default configuration will be applied, which can be overridden via
environment variables or by providing the appropriate beans yourself. The possible
configuration values are


  | @Bean name                         | Environnment variable name                   | Description                                              | Default value |
  |------------------------------------|----------------------------------------------|----------------------------------------------------------|---------------|
  | webSocketAdapterIdentifier         | NOVA.WEB_SOCKET.ADAPTER_IDENTIFIER           | the identifier to assign to the HttpAdapter.             | <null> |
  | | | | |
  | httpServer                         | n/a                                          | the ```HttpServer``` instance, handling the incoming communication. This is an optional bean. If not provided, the HttpAdapter can only be used in client mode.| <null> |
  | webSocketObjectMapper              | n/a                                          | the ObjectMapper to use when transcribing incoming / outgoing messages| default ObjectMapper, for details see [here](../comm/README.md) |
  | webSocketMessageTranscriber        | n/a                                          | the transcriber to use for incoming / outgoing messages  | default transcriber, for details see [here](../comm/README.md) |

As you can see from the table above, the WebSocketAdapter needs an ```HttpServer``` instance to listen to HTTP requests, which 
you have to provide as a bean in your ApplicationContext. This can be done very easily 
by importing the ```HttpServerProvidingConfiguration``` which will automatically provide the appropriate bean named "httpServer". 
The configuration options are  
   
  | @Bean name                         | Environnment variable name                   | Description                                              | Default value |
  |------------------------------------|----------------------------------------------|----------------------------------------------------------|---------------|
  | httpServerPort                     | NOVA.HTTP.SERVER.PORT                        | the port, the HTTP server listens on                     | 10000         |
  | httpServerInterfaceName            | NOVA.HTTP.SERVER.INTERFACE_NAME              | the interface, the HTTP server listens on                | "0.0.0.0"     |
  | httpServerKeyStore                 | NOVA.HTTP.SERVER.KEY_STORE                   | the keystore to use. Switches on SSL                     | <null>        |
  | httpServerKeyStorePass             | NOVA.HTTP.SERVER.KEY_STORE_PASS              | the password for the keystore                            | <null>        |
  | httpServerTrustStore               | NOVA.HTTP.SERVER.TRUST_STORE                 | the truststore to use to validate clients                | <null>        |
  | httpServerTrustStorePass           | NOVA.HTTP.SERVER.TRUST_STORE_PASS            | the password for the trust store                         | <null>        |
  | | | | |
  | httpServerConfigurationProperties                 | n/a                                          | an ```HttpServerSettings``` instance, containing all aforementioned config values. Handy if you want to read the configuration or override multiple defaults programmatically. |  |
   
Of course you can also instantiate a new ```WebSocketAdapter``` instance programmatically using its builder. 

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
