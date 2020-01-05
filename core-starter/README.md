core-starter
---

This package provides the spring boot starter and auto configuration for the Nova instance.

The possible configuration properties need to be prefixed with "nova." and can be looked up in the class
```ch.squaredesk.nova.autoconfig.NovaConfigurationProperties```. 

Currently, the following properties can be set:

```nova.identifier``` - The identifier of the Nova instance 

```nova.defaultBackpressureStrategy``` - The default backpressure strategy to use for the event Flowables. Default value is ```BUFFER```.

```nova.warnOnUnhandledEvent``` - Specifies if the system should log a warning when an event is emitted, for which no handler was registered. Default value is ```false```

```nova.captureJvmMetrics``` - Specifies if the system should capture VM metrics. Default value is ```true```
 
