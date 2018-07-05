# http-spring 


This is a little helper package that makes it easy to enable http communication
using spring ```Configuration``` classes. As the user of the API you can simply
import the specific ```Configuration``` classes and do not have to construct the beans 
yourself. As usual, overriding defaults is done via environment variables

The following classes are provided

* ```HttpServerConfigurationProvidedConfiguration```

  Yes, that's really its name (_naming things and cache invalidation..._).
  When you import this class in your own ```Configuration```, the app context will
  automatically contain a ```HttpServerConfiguration``` bean with sensible defaults:

  | Parameter / @Bean name   | Environnment variable name        | Description                               | Default value |
  |--------------------------|-----------------------------------|-------------------------------------------|---------------|
  | httpServerPort           | NOVA.HTTP.SERVER.PORT             | the port, the HTTP server listens on      | 10000         |
  | httpServerInterfaceName  | NOVA.HTTP.SERVER.INTERFACE_NAME   | the interface, the HTTP server listens on | "0.0.0.0"     |
  | httpServerKeyStore       | NOVA.HTTP.SERVER.KEY_STORE        | the keystore to use. Switches on SSL      | <null>        |
  | httpServerKeyStorePass   | NOVA.HTTP.SERVER.KEY_STORE_PASS   | the password for the keystore             | <null>        |
  | httpServerTrustStore     | NOVA.HTTP.SERVER.TRUST_STORE      | the truststore to use to validate clients | <null>        |
  | httpServerTrustStorePass | NOVA.HTTP.SERVER.TRUST_STORE_PASS | the password for the truststore           | <null>        |

  So, to override this values you can either set the appropriately named environment
  variables, or - if you prefer to code it - provide the appropriately named beans yourself.
    

* ```HttpServerProvidedConfiguration```

  When you import this class in your own ```Configuration```, the app context will
  automatically contain a ```HttpServer``` bean with sensible defaults. The defaults
  are created by importing the ```HttpServerConfigurationProvidedConfiguration```

* ```HttpServerStarter```

  This class is not a ```Configuration``` class. Instead, if you add a bean of this type
  to your application context, it will look for an ```HttpServer``` bean in your context
  and automatically ensure that the server is started once the app context has been refreshed.

    

