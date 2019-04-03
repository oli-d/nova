jms
=========

Reactive abstractions over JMS.

## 1. What is this about?
This package provides the ```JmsAdapter``` class which can be used to 
send and retrieve messages via JMS. Pub/sub, point-to-point as well as
RPC-style communication patterns are supported.

## 2. JmsAdapter instantiation

The easiest way to retrieve a ```JmsAdapter``` instance is using Spring. For this, we provide
the class ```JmsEnablingConfiguration```. Simply import this in your own Spring configuration
and you can het a bean called "jmsAdapter" from the ApplicationContext.

When you do so, default configuration will be applied, which can be overridden via
environment variables or by providing the appropriate beans yourself. The possible
configuration values are

  | @Bean name                         | Environnment variable name                   | Description                                              | Default value |
  |------------------------------------|----------------------------------------------|----------------------------------------------------------|---------------|
  | defaultHttpRequestTimeoutInSeconds | NOVA.HTTP.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS | the default timeout in seconds when firing HTTP requests | 30 |
  | defaultHttpAdapterIdentifier       | NOVA.HTTP.ADAPTER_IDENTIFIER                 | the identifier to assign to the HttpAdapter.             | <null> |
  | | | | |
  | httpServer                         | n/a                                          | the ```HttpServer``` instance, handling the incoming communication. This is an optional bean. If not provided, the HttpAdapter can only be used in client mode.| <null> |
  | httpMessageTranscriber             | n/a                                          | the transcriber to use for incoming / outgoing messages  | default transcriber, see below |
  | | | | |
  | httpServerStarter                  | n/a                                          | If you provide bean of class ```HttpServerStarter``` in your ApplicationContext, it will make sure that the server is automatically started as soon as you fire up the ApplicationContext.| <null> |

As described above, by default the HttpAdapter will only work in "client mode". If you want to listen to HTTP requests, 
you have to provide an ```HttpServer``` instance as a bean in your ApplicationContext. This can also be done very easily 
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
  | httpServerConfiguration            | n/a                                          | an ```HttpConfiguration``` instance, containing all aforementioned config values. Handy if you want to read the configuration or override multiple defaults programmatically. |  |
   

    
Of course you can also instantiate a new ```JmsAdapter``` instance programmatically. To do so,
make use of the ```Builder``` that can be obtained by invoking the builder method
 
```
Builder builder = JmsAdapter.builder();
```


## 3. Usage

...

### 3.3. Message "transcription"

Nessage transcription works exactly as described in detail in the [http](../http/README.md) module. 
