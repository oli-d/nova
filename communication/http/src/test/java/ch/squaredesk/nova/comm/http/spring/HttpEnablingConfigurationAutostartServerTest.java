package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.comm.http.HttpAdapter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Tag("medium")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HttpEnablingConfiguration.class})
class HttpEnablingConfigurationAutostartServerTest {
    @Autowired(required = false)
    private HttpServer httpServer;
    @Autowired
    private HttpAdapter httpAdapter;

    @BeforeAll
    static void setup() throws Exception  {
        System.setProperty("NOVA.HTTP.SERVER.AUTO_START", "false");
    }

    @AfterAll
    static void tearDown() throws Exception  {
        System.clearProperty("NOVA.HTTP.SERVER.AUTO_START");
    }

    @Test
    void serverNotStarted() throws Exception {
        Assertions.assertNotNull(httpAdapter);
        Assertions.assertNotNull(httpServer);
        Assertions.assertFalse(httpServer.isStarted());
    }

}