# annotation based REST communication

This package provides the ```OnRestRequest``` annotation. With this, you can conveniently register a REST request
handler method, simply by adding the annotation to your handling method. 

An example:

```
public class MyBean {
    @OnRestRequest("/foo")
    public String handle()  {
        return "MyBean says foo";
    }
}
```

If you declare your method this way, clients can send a REST request to ```"http://<your server>/foo"``` and
will receive the String "MyBean says foo" in the response body.

The following parameters are available to the ```OnRestRequest``` annotation:

| Parameter Name | Description | Default Value |
|----------------|-------------|---------------|
| value | the path, this handler is bound to. ___mandatory___ |  |
| consumes | media type, this handler consumes. | ```TEXT_PLAIN``` |
| produces | media type, this handler produces. | ```TEXT_PLAIN``` |
| requestMethod | HTTP request method, this handler supports | ```GET``` |

 
As it is the case for [event-annotations](../event-annotations/README.md), this feature relies on Spring
to work properly. We chose the same approach and also offer a specific ```Configuration``` class that 
can be used to conveniently enable it. Simply import the ```RestServerProvidingConfiguration``` class and you are
ready to go.
 
Doing so ensures, that a local server is started and all annotated handlers are properly registered.
 
The server is created with the following default settings:

| Parameter / @Bean name | Environnment variable name    | Description                               | Default value |
|------------------------|-------------------------------|-------------------------------------------|---------------|
| restPort               | NOVA.HTTP.REST.PORT           | the port, the HTTP server listens on      | 8080          |
| interfaceName          | NOVA.HTTP.REST.INTERFACE_NAME | the interface, the HTTP server listens on | "0.0.0.0"     |

So, to show you all this in action, here's how you would create a simple echo server:

1. Create a bean class that provides the echo handler
    
    ```
    public class EchoHandler {
        @OnRestRequest("/echo")
        public String echoParameterValue(@QueryParam("p1") String textFromCallerToEcho) {
            return textFromCallerToEcho;
        }
    
        @OnRestRequest("/echo/{text}")
        public String echoPathValue(@PathParam("text") String textFromCallerToEcho) {
            return textFromCallerToEcho;
        }
    
        @OnRestRequest(value = "/echo", requestMethod = HttpRequestMethod.POST)
        public String echoRequestObject(String textFromCallerToEcho) {
            return textFromCallerToEcho;
        }
    }
    ```
    
    Note that we register three different handlers:
    * ```echoParameterValue()``` expects a ```GET``` request and echoes the value of HTTP request parameter p1
    * ```echoPathValue()``` expects a ```GET``` request and echoes data parsed from the request path
    * ```echoRequestObject()``` expects a ```POST``` request and echoes the request body
    
1. Create a configuration class that provides the handler bean and enables REST handling
    
    ```
    @Configuration
    @Import(RestServerProvidingConfiguration.class)
    public class EchoConfiguration {
        @Bean
        public EchoHandler echoHandler() {
            return new EchoHandler();
        }
    }
    ```
    
1. Create a starter class
    
    ```
    public class EchoStarter {
        public static void main(String[] args) throws Exception {
            AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
            ctx.register(EchoConfiguration.class);
            ctx.refresh();
    
            // at this point, the REST server is started and properly initialized
            
            System.out.println("Echo server started. Press <ENTER> to stop the server...");
            System.in.read();
            ctx.getBean(HttpServer.class).shutdown();
        }
    }
    ```
        
1. If you're in doubt: test your server

   Using ```curl``` you can verify that our little server indeed does what we expect from it:
   
   * Running ```curl http://localhost:8080/echo?p1=data1``` will invoke ```echoParameterValue()``` 
   and return ```data3```
   * Running ```curl http://localhost:8080/echo/data2``` will invoke ```echoPathValue()``` 
   and return ```data2```
   * Running ```curl -X POST http://localhost:8080/echo -d "data3" -H "Content-Type:text/plain"```
    will invoke ```echoRequestObject()``` and return ```data3```
     