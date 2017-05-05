# annotation based event handling for Nova [![Build Status](https://travis-ci.org/oli-d/nova-event-annotations.svg?branch=master)](https://travis-ci.org/oli-d/nova-event-annotations)

This package provides the OnEvent annotation. With this, you can conveniently register an event handler method
in Nova's EventEmitter, simply by adding the annotation to your handling method. 

An example:

```
public class MyClass {
    ...
 
    @OnEvent("myEvent")
    public void myEventHandlerMethod (String parameter1, double parameter 2) {
        ...
    }
}
```

If you declare your method this way, it is called whenever the event "myEvent" is emitted. 

This feature is implemented using a specific Spring BeanPostProcessor. Therefore, to be able to use 
the functionality described above,
__the following beans must exist in the ApplicationContext:__ 
1. ___All of your beans that have been annotated___

1. ___A Nova instance (called "nova") which will take care of the EventDispatching___

1. ___Nova's EventHandlingBeanPostprocessor bean___

We do prefer annotation based ApplicationContext configurations, therefore we provide two convenience classes
that make your (and our) lives a lot easier.

```AnnotationEnablingConfiguration``` is a configuration class that properly instantiates the BeanPostProcessor. 
Simply import this in your specific Configuration class.

As indicated above, this class requires a Nova instance, so we also provide the ```NovaProvidingConfiguration``` class.
Import this, and a Nova instance with default configuration is provided. So, the easiest way to complete the
example is to provide a config like this:

```
@Configuration
@Import({NovaProvidingConfig.class, AnnotationEnablingConfiguration})
public class MyConfig {
    @Bean
    public MyClass getMyBean() {
        return new MyClass();
    }
    ... // further bean definitions
}
```

The default configuration provided by the ```NovaProvidingConfig``` class can easily be overridden using environment 
variables. The following table explains the supported parameters:

| Parameter name | Description | Possible Values | Default value |
|----------------|-------------|-----------------|---------------|
| ```NOVA.ID``` | The ID of the Nova instance | | ```null``` |
| ```NOVA.EVENTS.WARN_ON_UNHANDLED```| Specifies, whether a warning should be logged if an unhandled event is emitted | ```true```or ```false``` | ```false``` |
| ```NOVA.EVENTS.BACKPRESSURE_STRATEGY``` | Specifies which strategy to use when a producer emits faster than the consumer can process it. | One of the constants defined in ```io.reactivex.BackpressureStrategy```. See README.MD in the nova package for more details | ```BUFFER``` |

If you prefer defining the configuration in code, you can also make your Configuration class inherit from 
```NovaProvidingConfig``` and override the desired Bean providing methods, e.g.:

```
@Import(AnnotationEnablingConfiguration.class)
public static class MyConfig extends NovaProvidingConfiguration {
    @Bean
    public MyClass getMyBean() {
        return new MyClass();
    }
    ... // further bean definitions

    @Override
    @Bean
    public String getIdentifier() {
        return "My Service";
    }
}

```

__But wait... there's one more thing!__
 
Since we firmly believe that a modern software system must measure as much as possible, we wanted
to make it easy for every event handler to capture its own specific metrics. Our solution to this
is that your are able to add an additional parameter to your handling method. If you do so, and

- the parameter is of type ```EventContext```
- and it's the last parameter in your event handling method signature,

the system automatically injects an ```EventContext``` instance whenever the handler is invoked. This 
context object provides convenient access to Nova's ```Metrics``` and ```EventEmitter```.

As an example, we could change the code from above to look like this:
 
```
public class MyClass {
    ...
 
    @OnEvent("myEvent")
    public void myEventHandlerMethod (String parameter1, double parameter 2, EventContext context) {
        ...
        context.metrics.getCounter("myCounter").inc();
        ...
    }
}
```

This would have the effect, that whenever somebody emits the ```"myEvent"``` event, the ```myEventHandler```
method is called with an appropriate context object which - in our example - is used to increment the 
counter named ```myCounter```. 
