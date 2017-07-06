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

We do prefer annotation based ApplicationContext configurations, therefore we provide the convenience 
class ```AnnotationEnablingConfiguration``` which makes providing the required 
```EventHandlingBeanPostprocessor``` bean very easy. Simply import it in your custom configuration 
class and you are ready to go. As an example:

```
@Configuration
@Import(AnnotationEnablingConfiguration)
public class MyConfig {
    @Bean
    public MyClass getMyBean() {
        return new MyClass();
    }
    ... // further bean definitions
}
```

They same approach is taken to provide the required ```Nova``` bean. If you do not want
to manually create it and hare happy with the defaults, simply import ```NovaProvidingConfiguration```
in addition:

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

The following defaults will be configured for the Nova bean:
 
| Parameter / @Bean name | Environment variable name | Description | default value |
|------------------------|---------------------------|-------------|---------------|
| identifier | NOVA.ID | ID of the Nova instance. Useful if you use multiple in your VM | "" |
| warnOnUnhandledEvents | NOVA.EVENTS.WARN_ON_UNHANDLED | Log a warning if an event was emitted, for which no listener was registered. | false |
| defaultBackpressureStrategy | NOVA.EVENTS.BACKPRESSURE_STRATEGY | Default backpressure strategy to apply when dispatching events. One of the values defined in ```io.reactivex.BackpressureStrategy``` | BUFFER |

You can override them by either providing / overriding the specific beans in your configuration
or by setting the appropriate environment variable.

__But wait... there's one more thing!__
 
Since we firmly believe that a modern software system should capture as much metric data as 
possible, we wanted to make it easy for every event handler to capture its own specific metrics. 
Our solution to this is that your are able to add an additional parameter to your handling method. 
If you do so, and

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
