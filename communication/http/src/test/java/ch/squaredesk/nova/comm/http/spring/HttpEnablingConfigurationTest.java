package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.comm.http.HttpAdapter;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("medium")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HttpEnablingConfiguration.class, NovaProvidingConfiguration.class })
class HttpEnablingConfigurationTest {
    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private HttpAdapter adapter;

    @AfterEach
    void shutdownServer() throws Exception  {
        HttpServer server = ctx.getBean(HttpServer.class);
        if (server!=null) {
            server.shutdown().get();
        }
    }

    @Test
    void importingConfigCreatesHttpAdapterWithDefaultSettings() {
        assertNotNull(adapter);
    }

}