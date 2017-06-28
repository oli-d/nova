package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.Nova;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

class RestEnablingConfigurationTest {
    private void fireUp() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        ctx.getBean(Nova.class);
    }

    @BeforeEach
    void clearEnvironment() {
        System.clearProperty("NOVA.HTTP.REST.INTERFACE_NAME");
        System.clearProperty("NOVA.HTTP.REST.PORT");
    }

    @Test
    void instanceIsCreatedWithDefaultValuesWhenJustImportingConfig() throws Exception {
        fireUp();
        HttpHelper.waitUntilSomebodyListensOnPort(8888, 2, TimeUnit.SECONDS);
    }

    @Test
    void portCanBeOverridenWithEnvironmentVariable() throws Exception{
        System.setProperty("NOVA.HTTP.REST.PORT", "9999");
        fireUp();
        HttpHelper.waitUntilSomebodyListensOnPort(9999, 2, TimeUnit.SECONDS);
    }

    @Configuration
    @Import(RestEnablingConfiguration.class)
    public static class MyConfig {
        @Bean
        public Nova nova () {
            return Nova.builder().build();
        }
    }
}

