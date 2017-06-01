spring-support
---

This package brings Spring support to Nova.

It does so by,

1. (obviously) declaring the Spring dependency, and

1. provide class ```NovaProvidingConfiguration```, which can conveniently
  be used to add a Nova bean instance (named "nova") to the ApplicationContext

Just import ```NovaProvidingConfiguration``` in your custom configuration
class like in this code snippet:


    @Configuration
    @Import(NovaProvidingConfig.class)
    public class MyConfig {
        @Bean
        public MyClass getMyBean() {
            return new MyClass();
        }
        ... // further bean definitions
    }

will create a Nova instance with reasonable default values. The defaults
can easily be overridden using environment variables. The following table 
explains the supported parameters:

| Parameter name | Description | Possible Values | Default value |
|----------------|-------------|-----------------|---------------|
| ```NOVA.ID``` | The ID of the Nova instance | | ```null``` |
| ```NOVA.EVENTS.WARN_ON_UNHANDLED```| Specifies, whether a warning should be logged if an unhandled event is emitted | ```true``` or ```false``` | ```false``` |
| ```NOVA.EVENTS.BACKPRESSURE_STRATEGY``` | Specifies which strategy to use when a producer emits faster than the consumer can process it. | One of the constants defined in ```io.reactivex.BackpressureStrategy```. See README.MD in the nova package for more details | ```BUFFER``` |
| ```NOVA.METRICS.CAPTURE_VM_METRICS``` | Specifies whether JVM metrics should automatically be captured | ```true``` or ```false``` | ```true``` |

If you prefer defining the configuration in code, you can also make your custom 
configuration class inherit from ```NovaProvidingConfig``` and override the 
desired Bean providing methods, e.g.:

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
    public String identifier() {
        return "My Service";
    }
}

```
