package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("medium")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {HttpServerProvidingConfiguration.class, NovaProvidingConfiguration.class})
class HttpServerProvidingConfigurationTest {
    @Autowired
    private HttpServer httpServer;
    @Autowired
    private HttpServerSettings settings;

    @AfterEach
    void shutdownServer() throws Exception {
        if (httpServer!=null) {
            httpServer.shutdown().get();
        }
    }

    @Test
    void importingConfigCreatesServerWithDefaultSettings() {
        assertNotNull(httpServer);
        assertTrue(httpServer.isStarted());
        assertNotNull(settings);
        assertThat(settings.interfaceName, is("0.0.0.0"));
        assertThat(settings.port, is(10000));
    }

}