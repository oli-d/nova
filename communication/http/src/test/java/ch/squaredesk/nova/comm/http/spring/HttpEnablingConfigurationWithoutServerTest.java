package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.comm.http.HttpAdapter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Tag("medium")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HttpEnablingConfigurationWithoutServerTest.MyConfig.class})
class HttpEnablingConfigurationWithoutServerTest {
    @Autowired(required = false)
    private HttpServer httpServer;
    @Autowired
    private HttpAdapter httpAdapter;


    @AfterAll
    static void tearDown()  {
    }

    @AfterEach
    void shutdownServer() throws Exception {
        if (httpServer != null) {
            httpServer.shutdown().get();
        }
    }

    @Test
    void serverWasNotProvided() throws Exception {
        System.clearProperty(HttpServerProvidingConfiguration.BeanIdentifiers.AUTO_CREATE_SERVER);
        Assertions.assertNotNull(httpAdapter);
        Assertions.assertNull(httpServer);
    }

    @Import({HttpEnablingConfiguration.class})
    public static class MyConfig {
        @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.AUTO_CREATE_SERVER)
        public boolean auttoCreateHttpServer() {
            return false;
        }

    }
}