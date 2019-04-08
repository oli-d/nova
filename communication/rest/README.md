# rest

This package provides convenience classes to easily use the REST related annotations, defined
in the [JAX-RS](https://projects.eclipse.org/projects/ee4j.jaxrs) spec.

We chose the same approach as for all our other communication modules and offer a specific 
```Configuration``` class that can be used to conveniently enable the desired functionality. 
Simply import the ```RestEnablingConfiguration``` class and you are (almost) ready to go.

Doing so ensures, that a local server is started and all annotated handlers are properly registered.

The server is created with the default settings described in the [http](../http/README.md)
package.


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

### Configuration

As stated above, there's not much you need to configure. Just import ```RestEnablingConfiguration.class``` 
and you are almost ready to go.

The only problem left is to tell the framework where it can find the annotated
handler classes. Unfortunately, the underlying implementation does not allow registration of new handlers 
dynamically during runtime. They have to be known and provided at the time, the 
REST server is instantiated. To do so, it is enough to provide the name of the packages
that contain REST request handlers with the following bean or environment parameter:

  | @Bean name                    | Environnment variable name              | Description                                                                   | Default value |
  |-------------------------------|-----------------------------------------|-------------------------------------------------------------------------------|---------------|
  | restPackagesToScanForHandlers | NOVA.REST.PACKAGES_TO_SCAN_FOR_HANDLERS | A mandatory, list of package names that will (recursively) be scanned for REST request handlers. If programmatically providing a bean return a String array, if using the environment parameter, specify a comma separated list. | n/a|
 

Out of the box, JSON marshalling will be supported using Jackson. The default ObjectMapper
that is used for that will register all modules it can find on your classpath. If you need
a specifically configured ObjectMapper for the marshalling of your entities, you can provide
one in your Spring application context. Make sure that it is named "restObjectMapper", e.g.:

```Java
    @Bean("restObjectMapper")
    public ObjectMapper restObjectMapper() {
        return ...
    }
```

### Example: A Simple Echo Server

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
    
1. Create a configuration class that enables REST handling and specifies the name of the package conatining the 
handler class
    
    ```Java
    @Configuration
    @Import(RestEnablingConfiguration.class)
    public class EchoConfiguration {
        @Bean("restPackagesToScanForHandlers")
        public String[] restPackagesToScanForHandlers() {
            return new String[]{"ch.squaredesk.nova.comm.rest.example"};
        }
    }
    ```
    
    (Of course, you could also omit the list of packages and set the environment 
    variable ```NOVA.REST.PACKAGES_TO_SCAN_FOR_HANDLERS``` to ```ch.squaredesk.nova.comm.rest.example``` instead.)  
    
1. Create a starter class
    
    ```Java
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
   
   * Running ```curl http://localhost:10000/echo?p1=data1``` will invoke ```echoParameterValue()``` 
   and return ```data1```
   * Running ```curl http://localhost:10000/echo/data2``` will invoke ```echoPathValue()``` 
   and return ```data2```
   * Running ```curl -X POST http://localhost:10000/echo -d "data3" -H "Content-Type:text/plain"```
    will invoke ```echoRequestObject()``` and return ```data3```
     
