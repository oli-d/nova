# annotation based REST communication

This package provides convenience classes to easily use the REST related annotations, defined
in the [JAX-RS](https://projects.eclipse.org/projects/ee4j.jaxrs) spec.

As it is the case for [event-annotations](../event-annotations/README.md), this feature relies on Spring
to work properly. We chose the same approach and offer a specific ```Configuration``` class that
can be used to conveniently enable the desired functionality. Simply import the ```RestEnablingConfiguration```
class and you are ready to go.

Doing so ensures, that a local server is started and all annotated handlers are properly registered.

The server is created with the default settings described in the [http-ch.squaredesk.nova.spring](../http-spring/README.md)
package.


So, as an example, if you write the following bean:

```Java
@Path("/foo")
public class MyRestHandlerBean {
    @GET
    public String handle()  {
        return "MyBean says foo";
    }
}
```

and include it in the Spring application context, clients are able to send a REST request
to ```"http://<your server>/foo"``` and will receive the String "MyBean says foo" in the response body.

For detailed usage and enhanced features like async request processing, please refer to the
[Jersey](http://jersey.github.io/) documentation, which is used as the underlying implementation.

### Configuration

There's not much you need to configure. Just import ```RestEnablingConfiguration.class``` and you are
ready to go.

By default, the REST server will be started as soon as the application context
has been initialized. If you want to switch that feature off and start the
httpServer manually at a later point in time, you can set the environment
variable ```NOVA.HTTP.REST.SERVER.AUTO_START``` to false or provide your own bean named ```autoStartRestServer```.

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

1. Create a bean class that provides the echo handler
    
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
    
1. Create a configuration class that provides the handler bean and enables REST handling
    
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
     
### Mixing rest and http

Using the ```@OnRestRequest``` ch.squaredesk.nova.events.ch.squaredesk.nova.events.annotation is very convenient and enables you to expose your server
side functionality in a very simple way, requiring almost no additional code. 

Unfortunately, you do not have much control over how the requests get processed, e.g. you are
not able to control threading or apply a specific backpressure strategy.

For that reason, it might be desirable to mix the convenience of the rest package with
the control of the [http](../http/README-md) communication package. Since both make use of the same 
```SimpleHttpServer```, this is totally possible. However, there's one gotcha you need to be aware of:

To be able to register your annotated REST handlers, all affected Spring beans must be instantiated
__BEFORE__ the ```SimpleHttpServer``` instance is created. The underlying [grizzly](https://javaee.github.io/grizzly/)
library we use does not allow the addition of new REST handlers after the server instance was created. This is in 
contrast to "normal" HTTP handlers (used in the [http](../http/README-md) communication package), 
that are allowed to be added at any time.

The result of this is that we have to take care of how, resp. when the ```HTTPServer``` is instantiated.
We try to do this in ```RestEnablingConfiguration```, but the ch.squaredesk.nova.spring gods do unfortunately not give us full
control over the process. Therefore:
- make sure that you do not create your own ```SimpleHttpServer``` bean anywhere else
- and be warned that there might (luckily very very rare) occasions, where ch.squaredesk.nova.spring will complain during application 
context creations. Unfortunately in those rare situations, you cannot use the convenience classes and have
to construct all beans manually :(
