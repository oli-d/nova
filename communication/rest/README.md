# rest

This package provides convenience classes to easily use the REST related annotations, defined
in the [JAX-RS](https://projects.eclipse.org/projects/ee4j.jaxrs) spec.

We chose the same approach as for all our other communication modules and offer a specific 
```Configuration``` class that can be used to conveniently enable the desired functionality. 
Simply import the ```RestEnablingConfiguration``` class and you are ready to go.

Doing so ensures, that a local server is started and all annotated handlers are properly registered.

The server is created with the default settings described in the [http](../http/README.md)
package. In addition, you can define the following REST specific configuration:

| Environnment variable / bean name | Description                   | Default value |
|-----------------------------------|-------------------------------|---------------|
| CAPTURE_METRICS                   | Enable metrics capturing      | ```true``` |
| NOVA.REST.LOG_INVOCATIONS         | Log REST endpoint invocations | ```true``` |
| NOVA.REST.SERVER.PROPERTIES       | A collection of ```Pair<String, Object>``` entities, that set specific server properties. For details, check the documentation of [Jersey](https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest), the underlying implementation we use  | _empty_        |


So, as an example, if you write the following class:

```Java
@Path("/foo")
public class MyRestHandlerBean {
    @GET
    public String handle()  {
        return "MyBean says foo";
    }
}
```

clients are able to send a REST request
to ```"http://<your server>/foo"``` and will receive the String "MyBean says foo" in the response body.

For detailed usage and enhanced features like async request processing, please refer to the
[Jersey](http://jersey.github.io/) documentation, which is used as the underlying implementation.

## Configuration

As stated above, there's not much you need to configure. Just @Import ```RestEnablingConfiguration.class``` 
and you are ready to go.

## Example: A Simple Echo Server

As a bit more complete example, here's how you would create a simple echo server:

1. Create a class that provides the echo handler
    
    ```Java
    @Path("/echo")
    public class EchoHandler {
        @GET
        public String echoParameterValue(@QueryParam("p1") String textFromCallerToEcho) {
            return textFromCallerToEcho;
        }
    
        @Path("/{text}")
        public String echoPathValue(@PathParam("text") String textFromCallerToEcho) {
            return textFromCallerToEcho;
        }
    
        @POST
        public String echoPostRequestBody(String textFromCallerToEcho) {
            return textFromCallerToEcho;
        }
    }
    ```
    
    Note that we register three different handlers:
    * ```echoParameterValue()``` expects a ```GET``` request and echoes the value of HTTP request parameter p1
    * ```echoPathValue()``` expects a ```GET``` request and echoes data parsed from the request path
    * ```echoPostRequestBody()``` expects a ```POST``` request and echoes the request body
    
1. Create a configuration class that enables REST handling and provide the handler bean
    
    ```Java
    @Configuration
    @Import(RestEnablingConfiguration.class)
    public class EchoConfiguration {
        @Bean
        public EchoHandler echoHandler() {
            return new EchoHandler();
        }
    }
    ```
    
1. Create a starter class
    
    ```Java
    public class EchoStarter {
        public static void main(String[] args) throws Exception {
            try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
                ctx.register(EchoConfiguration.class);
                ctx.refresh();
    
                // at this point, the REST server is started and properly initialized
    
                System.out.println("Echo server started. Press <ENTER> to stop the server...");
                System.in.read();
            }
        }
    }
    ```
        
1. If you're in doubt: test your server

   Using ```curl``` you can verify that our little server indeed does what we expect from it:
   
   * Running ```curl http://localhost:10000/echo?p1=data1``` will invoke ```echoParameterValue()``` 
   and return ```data1```
   * Running ```curl http://localhost:10000/echo/data2``` will invoke ```echoPathValue()``` 
   and return ```data2```
   * Running ```curl -X POST http://localhost:10000/echo -d "data3" -H "Content-Type:text/plain"```
    will invoke ```echoRequestObject()``` and return ```data3```
     
