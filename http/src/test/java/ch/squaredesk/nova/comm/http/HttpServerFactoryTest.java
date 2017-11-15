package ch.squaredesk.nova.comm.http;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpServerFactoryTest {
    HttpServerConfiguration serverConfig;
    ResourceConfig resourceConfig;

    @BeforeEach
    void setup() {
        serverConfig = HttpServerConfiguration.builder().interfaceName("localhost").port(5678).build();
        resourceConfig = new ResourceConfig();
        resourceConfig.registerResources(Resource.builder().build());
    }
    @Test
    void serverCantBeCreatedWithoutConfig() {
        assertThrows(NullPointerException.class,
                () -> HttpServerFactory.serverFor(null));
    }

}