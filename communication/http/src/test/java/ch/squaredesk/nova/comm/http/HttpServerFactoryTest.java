/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("medium")
class HttpServerFactoryTest {
    @Test
    void serverCantBeCreatedWithoutConfig() {
        assertThrows(NullPointerException.class,
                () -> HttpServerFactory.serverFor(null));
    }

    @Test
    void serverNotAutomaticallyStarted() {
        HttpServerSettings rsc = HttpServerSettings.builder().interfaceName("localhost").port(10000).build();
        HttpServer httpServer = HttpServerFactory.serverFor(rsc);
        MatcherAssert.assertThat(httpServer.isStarted(), Matchers.is(false));
    }

}