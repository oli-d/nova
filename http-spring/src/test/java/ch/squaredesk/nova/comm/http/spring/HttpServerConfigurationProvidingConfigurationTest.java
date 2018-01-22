package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HttpServerConfigurationProvidingConfigurationTest {
    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    private void setupContext(Class configClass) {
        ctx.register(configClass);
        ctx.refresh();
    }

    @AfterEach
    void cleanEnvironmentVariables() {
        System.clearProperty("NOVA.HTTP.SERVER.PORT");
        System.clearProperty("NOVA.HTTP.SERVER.INTERFACE_NAME");
    }

    @Test
    void importingConfigCreatesDefaultSettings() {
        setupContext(MyDefaultConfig.class);

        HttpServerConfiguration cfg = ctx.getBean(HttpServerConfiguration.class);

        assertNotNull(cfg);
        assertThat(cfg.interfaceName, is("0.0.0.0"));
        assertThat(cfg.port, is(10000));
    }

    @Test
    void defaultSettingsCanBeOverriddenWithEnvironmentVariables() {
        System.setProperty("NOVA.HTTP.SERVER.PORT", "1234");
        System.setProperty("NOVA.HTTP.SERVER.INTERFACE_NAME", "oli");
        setupContext(MyDefaultConfig.class);

        HttpServerConfiguration cfg = ctx.getBean(HttpServerConfiguration.class);

        assertNotNull(cfg);
        assertThat(cfg.interfaceName, is("oli"));
        assertThat(cfg.port, is(1234));
    }

    @Test
    void defaultSettingsCanBeOverriddenWithProvidingOurOwnBeans() {
        setupContext(MyOverridingConfig.class);

        HttpServerConfiguration cfg = ctx.getBean(HttpServerConfiguration.class);

        assertNotNull(cfg);
        assertThat(cfg.interfaceName, is("xxx"));
        assertThat(cfg.port, is(2345));
    }

    @Import({HttpServerConfigurationProvidingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyDefaultConfig {
    }

    @Import({HttpServerConfigurationProvidingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyOverridingConfig {
        @Bean("httpServerPort")
        public Integer httpServerPort() {
            return 2345;
        }

        @Bean("httpServerInterfaceName")
        public String interfaceName() {
            return "xxx";
        }
    }
}