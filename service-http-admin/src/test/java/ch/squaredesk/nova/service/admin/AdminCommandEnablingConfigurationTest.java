package ch.squaredesk.nova.service.admin;

import ch.squaredesk.nova.Nova;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AdminCommandEnablingConfigurationTest {
    private void fireUp() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        ctx.getBean(Nova.class);
    }

    @BeforeEach
    void clearEnvironment() {
        System.clearProperty("NOVA.ADMIN.INTERFACE_NAME");
        System.clearProperty("NOVA.ADMIN.PORT");
        System.clearProperty("NOVA.ADMIN.BASE_URL");
    }

    @Test
    void instanceIsCreatedWithDefaultValuesWhenJustImportingConfig() throws Exception {
        fireUp();
        HttpHelper.waitUntilSomebodyListensOnPort(9999, 2, TimeUnit.SECONDS);
    }

    @Test
    void portCanBeOverridenWithEnvironmentVariable() throws Exception{
        System.setProperty("NOVA.ADMIN.PORT", "9999");
        fireUp();
        HttpHelper.waitUntilSomebodyListensOnPort(9999, 2, TimeUnit.SECONDS);
    }

    @Configuration
    @Import(AdminCommandEnablingConfiguration.class)
    public static class MyConfig {
        @Bean
        public Nova nova () {
            return Nova.builder().build();
        }
    }
}

