package ch.squaredesk.nova.comm.http;

import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpServerFactoryTest {
    @Test
    void serverCantBeCreatedWithoutConfig() {
        assertThrows(NullPointerException.class,
                () -> HttpServerFactory.serverFor(null));
    }

    @Test
    void serverNotAutomaticallyStarted() {
        HttpServerConfiguration rsc = HttpServerConfiguration.builder().interfaceName("localhost").port(10000).build();
        HttpServer httpServer = HttpServerFactory.serverFor(rsc);
        MatcherAssert.assertThat(httpServer.isStarted(), Matchers.is(false));
    }

}