package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HttpServerProvidingConfigurationTest {
    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    private void setupContext(Class configClass) {
        ctx.register(configClass);
        ctx.refresh();
    }

    @AfterEach
    void shutdownServer() throws Exception  {
        HttpServer server = ctx.getBean(HttpServer.class);
        if (server!=null) {
            server.shutdown().get();
        }
    }

    @Test
    void importingConfigCreatesServerWithDefaultSettings() {
        setupContext(DefaultConfig.class);

        HttpServer server = ctx.getBean(HttpServer.class);

        assertNotNull(server);
        assertThat(server.isStarted(), is(false));
    }

    @Test
    void providingServerStarterBeanAutomaticallyStartsServer() {
        setupContext(ConfigWithStarter.class);

        HttpServer server = ctx.getBean(HttpServer.class);

        assertNotNull(server);
        assertThat(server.isStarted(), is(true));
    }

    @Import({HttpServerProvidingConfiguration.class, NovaProvidingConfiguration.class})
    public static class DefaultConfig {
    }

    @Import({HttpServerProvidingConfiguration.class, NovaProvidingConfiguration.class})
    public static class ConfigWithStarter {
        @Bean
        public HttpServerStarter serverStarter() {
            return new HttpServerStarter();
        }
    }
}