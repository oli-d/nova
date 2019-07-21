package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.comm.http.HttpServerSettings;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("medium")
class HttpServerProvidingConfigurationWithCustomSettingsTest {
    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    private void setupContext(Class configClass) {
        ctx.register(configClass);
        ctx.refresh();
    }

    @AfterEach
    void cleanEnvironmentVariables() {
        System.clearProperty(HttpServerProvidingConfiguration.BeanIdentifiers.PORT);
        System.clearProperty(HttpServerProvidingConfiguration.BeanIdentifiers.INTERFACE);
    }

    @Test
    void defaultSettingsCanBeOverriddenWithEnvironmentVariables() {
        System.setProperty(HttpServerProvidingConfiguration.BeanIdentifiers.PORT, "1234");
        System.setProperty(HttpServerProvidingConfiguration.BeanIdentifiers.INTERFACE, "oli");
        setupContext(EmptyConfig.class);

        HttpServerSettings cfg = ctx.getBean(HttpServerSettings.class);

        assertNotNull(cfg);
        assertThat(cfg.interfaceName, is("oli"));
        assertThat(cfg.port, is(1234));
    }

    @Test
    void defaultSettingsCanBeOverriddenWithProvidingOurOwnBeans() {
        setupContext(MyOverridingConfig.class);

        HttpServerSettings cfg = ctx.getBean(HttpServerSettings.class);

        assertNotNull(cfg);
        assertThat(cfg.interfaceName, is("xxx"));
        assertThat(cfg.port, is(2345));
    }

    @Import(HttpServerProvidingConfiguration.class)
    public static class EmptyConfig {
        @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.SERVER)
        public HttpServer httpServer() {
            return null;
        }
    }

    @Import(HttpServerProvidingConfiguration.class)
    public static class MyOverridingConfig {
        @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.PORT)
        public Integer httpServerPort() {
            return 2345;
        }

        @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.INTERFACE)
        public String interfaceName() {
            return "xxx";
        }

        @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.SERVER)
        public HttpServer httpServer() {
            return null;
        }
    }
}