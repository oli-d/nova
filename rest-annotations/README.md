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
| consumes | media type, this handler consumes. | ```APPLICATION_JSON``` |
| produces | media type, this handler produces. | ```APPLICATION_JSON``` |
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

So, to show you a very simple example, here's how you would create a simple echo server:

1. Create a bean class that provides the echo handler
    
    ```
    ```
    
1. Create a configuration class that provides the handler bean and enables REST handling
    
    ```
    ```
    
1. Create a starter class
    
    ```
    ```
    
