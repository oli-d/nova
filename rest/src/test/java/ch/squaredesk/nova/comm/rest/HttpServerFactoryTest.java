package ch.squaredesk.nova.comm.rest;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpServerFactoryTest {
    RestServerConfiguration serverConfig;
    ResourceConfig resourceConfig;

    @BeforeEach
    void setup() {
        serverConfig = new RestServerConfiguration(5678, "localhost");
        resourceConfig = new ResourceConfig();
        resourceConfig.registerResources(Resource.builder().build());
    }
    @Test
    void serverCantBeCreatedWithoutConfig() {
        assertThrows(NullPointerException.class,
                () -> HttpServerFactory.serverFor(null, resourceConfig));
    }

    @Test
    void serverCantBeCreatedWithNullResources() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpServerFactory.serverFor(serverConfig, null));
    }

    @Test
    void serverCantBeCreatedWithEmptyResources() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpServerFactory.serverFor(serverConfig, new ResourceConfig()));
    }
}