package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.HttpAdapter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

@Tag("medium")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HttpEnablingConfigurationWithoutServerTest.MyConfig.class})
class HttpEnablingConfigurationWithoutServerTest {
    @Autowired(required = false)
    private HttpServer httpServer;
    @Autowired
    private HttpAdapter httpAdapter;

    @BeforeAll
    static void setup() {
        System.setProperty("NOVA.HTTP.SERVER.AUTO_CREATE", "false");
    }

    @AfterAll
    static void tearDown()  {
    }

    @Test
    void serverWasNotProvided() throws Exception {
        System.clearProperty("NOVA.HTTP.SERVER.AUTO_CREATE");
        Assertions.assertNotNull(httpAdapter);
        Assertions.assertNull(httpServer);
    }

    @Import({HttpEnablingConfiguration.class})
    public static class MyConfig {
        @Bean("autoCreateHttpServer")
        public boolean auttoCreateHttpServer() {
            return false;
        }

    }
}