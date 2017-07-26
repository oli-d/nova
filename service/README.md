# nova-service

This artifact provides ```NovaService```, an abstract class that enables you to
quickly create a standalone Java service.

To do so, all you have to do is
1. Create a service main class which inherits from ```NovaService```
1. Implement a Spring  configuration class that inherits from ```NovaServiceConfiguration```

What you get is
* a service identifier
* a simple service lifecycle
* a ```Nova``` instance, which provides out-of-the-box service metrics and reactive access
to the event bus and file system
* annotation based event handling and
* easy configuration management

### The Service identifier

Each service has a ```serviceId``` and a ```serviceName``` field. The values of this
field can be set by
* either providing appropriately named beans in the Spring application context,
* or setting the environment variables ```NOVA.SERVICE.INSTANCE_ID``` and ```NOVA.SERVICE.NAME```

If you do not implement either of the mentioned solutions, the system will calculate the
values automatically. The ServiceName is derived by inspecting your service' class name and
the instanceId will be a new UUID.

Please note that it is recommended to have a unique tuple of service name and instance ID. The
framework will not enforce this, but in a multi-instance environment failing to do so will cause
headaches trying to properly monitor system metrics.

### The Service lifecycle

The service can be in one of the following three states: new, initialized or started.

You can switch between those states by calling one of the following methods
* ```createInstance()``` - creates a new instance and initializes it
* ```start()``` - changes the state from initialized to started. ```start()``` blocks the
calling thread, i.e. as long as you invoke this method from a non-daemon thread, your VM
will not exit
* ```shutdown()``` - changes the state from started to initialized. Calling this method
unblocks the thread that invoked ```start()```

For each of those state transition a callback method is invoked that can be implemented
by the concrete sub classes:
* ```onInit()```
* ```onStart()```
* ```onShutdown()```

### The ```Nova``` instance

The ```Nova``` instance is a protected field in the ```NovaService``` class. To see what you
can do with it, check the documentation of the [nova core](../core/README.md) artifact.

### Annotation-based event handling

This is enbaled by default. For details check [event-annotations](../event-annotations/README.md) artifact.

### Configuration management

When the service instance is initialized, it automatically tries to load the file
```defaults.properties``` from the classpath. If this file exists, all defined properties
are available in Spring's ```Environment``` and can be used in your configuration or
other classes.

In addition to that, it is possible to load an additional config file by assigning the
path to the environment variable ```NOVA.SERVICE.CONFIG```. The system tries to find this
file in the local file system and on the classpath (in that order).

If the additional config file is found, the defined properties are also added to the Spring
```Environment```, overriding those entries that were already defined in ```default.properties```.

Last, but not least (since we are using Spring), it is also possible
to declare (and override default) configuration properties by setting the appropriately
named environment variable.

With this approach it is easy to provide a packaged service with default configuration and
adapt it to a specific runtime environment by either providing a specific configuration file
or specifically overriding single configuration values by setting the appropriate environment
variables.

## An Example: Time server

To tie all this together, let's look at an example:

Our goal is to build a simple server that offers a REST endpoint to get the local time. The time
is returned as a String with a configurable prefix.

So let's start with implementing the business logic. For that, we create the new class
```TimeRequestHandler```:

```Java
public class TimeRequestHandler {
    public final String messagePrefix;

    public TimeRequestHandler(String messagePrefix) {
        this.messagePrefix = messagePrefix;
    }

    @OnRestRequest("/time")
    public String time() {
        return messagePrefix + " " + LocalDateTime.now() + "\n";
    }
}
```

Nothing fancy here (for details regarding the ```@OnRestRequest``` annotation, check out
the [rest-annotations](../rest-annotations/README.md) module), so let's move on and make this
a real service.

First thing we need is an appropriate "main" or "starter" class, let's call it ```TimeService```:

```Java
public class TimeService extends NovaService {
    private final HttpServer httpServer;

    public TimeService(HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    @Override
    protected void onShutdown() {
        httpServer.shutdown();
    }

    public static void main(String[] args) {
        TimeService time = NovaService.createInstance(TimeService.class, TimeServiceConfig.class);
        time.start();
        System.out.printf("TimeService started, press <ENTER> to shutdown");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        time.shutdown();
    }
}
```
Again - as you can see - there's not much to do. In the ```main()``` method we create a new
instance by passing the appropriate config class and resulting service class

```Java
    TimeService time = NovaService.createInstance(TimeService.class, TimeServiceConfig.class);
```

The rest of the ```main()``` method just waits for the user to press < ENTER > and - when
done - shuts down the service.

As mentioned previously, when the service shuts down, the ```onShutdown()``` callback
is invoked. Our implementation shuts down the HTTP server which is used to process
the REST requests, so that we cleanly exit the VM:

```Java
@Override
protected void onShutdown() {
    httpServer.shutdown();
}
```

Now the only thing left to implement is the service configuration class that provides all
required beans:
```Java
@Configuration
@Import(RestServerProvidingConfiguration.class)
public class TimeServiceConfig extends NovaServiceConfiguration<TimeService> {
    @Autowired
    Environment env;

    @Autowired
    @Lazy
    HttpServer restHttpServer;

    @Override
    @Bean
    public TimeService serviceInstance() {
        return new TimeService(restHttpServer);
    }

    @Bean
    public TimeRequestHandler timeRequestHandler() {
        return new TimeRequestHandler(env.getProperty("messagePrefix",""));
    }
}
```

The first thing to mention is that we inherit from ```NovaServiceConfiguration```, a generic class
that is parametrized with the concrete class of the configured service (```TimeService``` in
our example).

```NovaServiceConfiguration``` is an abstract class and requires us to implement
```serviceInstance()``` to return the service bean:
```Java
@Override
@Bean
public TimeService serviceInstance() {
    return new TimeService(restHttpServer);
}
```

We pass the ```httpServer``` instance to the constructor, so that we can properly
implement ```onShutdown()``` as described above. The instance is available and can
be ```@Autowired``` since we import ```RestServerProvidingConfiguration.class```.

Finally, we also create the bean that handles the REST requests:
```Java
@Bean
public TimeRequestHandler timeRequestHandler() {
    return new TimeRequestHandler(env.getProperty("messagePrefix",""));
}
```

The ```messagePrefix``` parameter is passed to appropriately prefix the time
message. But where does that prefix come from? Well, we have a few different
choices to provide the appropriate value.

The first one is that we can add ```defaults.properties``` to the classpath:
```Java
messagePrefix=default
NOVA.HTTP.REST.PORT=9999
```
We set the message prefix to "default" and change the port on which we want to listen to
the REST requests to 9999.

With that we are able to start the service simply by passing the main class ```TimeService``` to
the JVM. Doing so starts our service, which we can test by invoking the following ```curl``` command:
```Java
curl localhost:9999/time
```
You will receive an answer similar to

```
default 2017-07-26T23:05:24.786
```

Great success! We got proof that our service was successfully started and our configuration
was properly applied. :-)

Now imagine, somebody sent us an executable jar file ```timeservice.jar```, which contains our
service plus default configuration. Unfortunately, when we try to run it, we realize that
port 9999 is already in use by another service, so that the ```TimeService``` is not starting up
properly. Luckily, we have to ways to override the default port 9999. The first one is that we
can set the appropriate OS environment variable or pass a VM parameter when invoking java:

```
java -DNOVA.HTTP.REST.PORT=8888 -jar timeservice.jar
```

Assuming port 8888 is not in use, the service can now properly start up.

In addition to the port, we can also override the message prefix:

```
java -DNOVA.HTTP.REST.PORT=8888 -DmessagePrefix=myPrefix -jar timeservice.jar
```

As the command can become very long if we have a lot of parameters to specify,
we can also define all of them in a separate config file (let's
say ```/tmp/myconfig.properties```) and define this on the command line:

```
java -DNOVA.SERVICE.CONFIG=/tmp/myconfig.properties -jar timeservice.jar
```

_(You can find the full source code of the example in the folder ```src/test/example/time```
in this repository)_