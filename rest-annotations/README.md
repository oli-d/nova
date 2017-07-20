# annotation based REST communication

---

This package provides the ```OnRestRequest``` annotation. With this, you can conveniently register a REST request
handler method, simply by adding the annotation to your handling method. 

### Usage

Let's start with an example:

<<<<<<< HEAD
```Java
=======
```
>>>>>>> admin-work
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
can be used to conveniently enable it. Simply import the ```RestServerProvidingConfiguration``` class and you are
ready to go.
 
Doing so ensures, that a local server is started and all annotated handlers are properly registered.
 
The server is created with the following default settings:

| Parameter / @Bean name | Environnment variable name    | Description                               | Default value |
|------------------------|-------------------------------|-------------------------------------------|---------------|
<<<<<<< HEAD
| restPort               | NOVA.HTTP.REST.PORT           | the port, the HTTP server listens on      | 10000         |
=======
| restPort               | NOVA.HTTP.REST.PORT           | the port, the HTTP server listens on      | 8080          |
>>>>>>> admin-work
| interfaceName          | NOVA.HTTP.REST.INTERFACE_NAME | the interface, the HTTP server listens on | "0.0.0.0"     |


### Example: A Simple Echo Server
So, to show all of this in action, here's how you would create a simple echo server:

1. Create a bean class that provides the echo handler
    
<<<<<<< HEAD
    ```Java
=======
    ```
>>>>>>> admin-work
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
    
<<<<<<< HEAD
    ```Java
=======
    ```
>>>>>>> admin-work
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
    
<<<<<<< HEAD
    ```Java
=======
    ```
>>>>>>> admin-work
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
   
<<<<<<< HEAD
   * Running ```curl http://localhost:10000/echo?p1=data1``` will invoke ```echoParameterValue()``` 
   and return ```data1```
   * Running ```curl http://localhost:10000/echo/data2``` will invoke ```echoPathValue()``` 
   and return ```data2```
   * Running ```curl -X POST http://localhost:10000/echo -d "data3" -H "Content-Type:text/plain"```
=======
   * Running ```curl http://localhost:8080/echo?p1=data1``` will invoke ```echoParameterValue()``` 
   and return ```data1```
   * Running ```curl http://localhost:8080/echo/data2``` will invoke ```echoPathValue()``` 
   and return ```data2```
   * Running ```curl -X POST http://localhost:8080/echo -d "data3" -H "Content-Type:text/plain"```
>>>>>>> admin-work
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

To be able to control the creation of the ```HttpServer``` bean as well as its lifecycle it is therefore 
not feasible to import ```RestServerProvidingConfiguration``` as in the examples above. For that reason we provide 
yet another ```Configuration``` class called ```RestEnablingConfiguration``` . This class provides the default server 
settings (via an appropriate ```HttpServerConfiguration``` bean) as described above and the required 
```BeanPostProcessor```, but does __NOT__ automatically create nor start the ```HttpServer``` bean. 

This in fact has to be done in your custom config after all REST handler beans were instantiated. It can easily
be done by passing the aforementioned ```HttpServerConfiguration``` bean to the ```RestServerFactory```. 

A common pattern is to split the configuration of your REST handler beans and RpcServer + HttpServer beans into two 
separate ```Configuration``` classes and use the ```@Order``` annotation on the latter one to signal Spring that it
should process this as late as possible:

<<<<<<< HEAD
```Java
=======
```
>>>>>>> admin-work
    @Configuration
    @Order
    @Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class, MyBeanConfig.class})
    public class MyServiceConfig  {
        @Autowired
        ResourceConfig resourceConfig;

        @Autowired
        HttpServerConfiguration serverConfig;

        @Autowired
        public Nova nova;

        @Bean
        public HttpServer httpServer() {
            return RestServerFactory.serverFor(serverConfig, resourceConfig);
        }

        @Bean
        @Lazy
        public RpcServer<String> rpcServer() {
            return new RpcServer<>(httpServer(), s->s, s->s, nova.metrics );
        }
    }
```

* We only define the HTTP related beans in ```MyServiceConfiguration``` and import all other beans from additional
```Configuration``` classes. 
* ```MyBeanConfig``` is providing all REST handler and other beans. 
* ```NovaProvidingConfiguration``` is imported to have the ```Nova``` bean available, which is required to create the 
```RpcServer```. 
* The ```HttpServerConfiguration``` bean is provided by the ```RestEnablingConfiguration``` and is 
required to be able to instantiate the ```HttpServer```, which in turn is also required by ```the RpcServer```. 
* ```MyServiceConfiguration``` is using the ```@Order``` annotation to signal the Spring framework that the creation 
<<<<<<< HEAD
of its beans should take lowest precedence.
=======
of its beans should take lowest precedence.
>>>>>>> admin-work
