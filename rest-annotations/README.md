# annotation based REST communication

This package provides the ```OnRestRequest``` annotation. With this, you can conveniently register a REST request
handler method, simply by adding the annotation to your handling method. 

### Usage

Let's start with an example:

```Java
public class MyBean {
    @OnRestRequest("/foo")
    public String handle()  {
        return "MyBean says foo";
    }
}
```

If you annotate your method as shown above, clients are able to send a REST request to ```"http://<your server>/foo"``` 
and will receive the String "MyBean says foo" in the response body.

The following parameters are available to the ```OnRestRequest``` annotation:

| Parameter Name | Description | Default Value |
|----------------|-------------|---------------|
| value | the path, this handler is bound to. ___mandatory___ |  |
| consumes | media type, this handler consumes. | ```TEXT_PLAIN``` |
| produces | media type, this handler produces. | ```TEXT_PLAIN``` |
| requestMethod | HTTP request method, this handler supports | ```GET``` |

 
As it is the case for [event-annotations](../event-annotations/README.md), this feature relies on Spring
to work properly. We chose the same approach and offer a specific ```Configuration``` class that 
can be used to conveniently enable it. Simply import the ```RestEnablingConfiguration``` class and you are
ready to go.
 
Doing so ensures, that a local server is started and all annotated handlers are properly registered.
 
The server is created with the default settings described in the [http-spring](../http-spring/README.md) 
package.


### Example: A Simple Echo Server
So, to show all of this in action, here's how you would create a simple echo server:

1. Create a bean class that provides the echo handler
    
    ```Java
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
    
    ```Java
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
     
### Mixing rest-annotations and http

Using the ```@OnRestRequest``` annotation is very convenient and enables you to expose your server
side functionality in a very simple way, requiring almost no additional code. 

Unfortunately, you do not have much control over how the requests get processed, e.g. you are
not able to control threading or apply a specific backpressure strategy.

For that reason, it might be desirable to mix the convenience of the rest-annotations package with 
the control of the [http](../http/README-md) communication package. Since both make use of the same 
```HttpServer```, this is totally possible. However, there's one gotcha you need to be aware of:

To be able to register your annotated REST handlers, all affected Spring beans must be instantiated
__BEFORE__ the ```HttpServer``` instance is created. The underlying [grizzly](https://javaee.github.io/grizzly/) 
library we use does not allow the addition of new REST handlers after the server instance was created. This is in 
contrast to "normal" HTTP handlers (used in the [http](../http/README-md) communication package), 
that are allowed to be added at any time.

The result of this is that we have to take care of how, resp. when the ```HTTPServer``` is instantiated.
We try to do this in ```RestEnablingConfiguration```, but the spring gods do unfortunately not give us full
control over the process. Therefore:
- make sure that you do not create your own ```HttpServer``` bean anywhere else
- and be warned that there might (luckily very very rare) occasions, where spring will complain during application 
context creations. Unfortunately in those rare situations, you cannot use the convenience classes and have
to construct all beans manually :(
