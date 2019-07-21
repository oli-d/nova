/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

public class SimpleHttpServer implements AutoCloseable {
    private final HttpServer delegate;

    private SimpleHttpServer(int port, String path, Consumer<HttpExchange> exchangeConsumer) throws IOException {
        delegate = HttpServer.create(new InetSocketAddress(port), 0);
        delegate.createContext(path, new MyHandler(exchangeConsumer));
        delegate.setExecutor(null); // creates a default executor
        delegate.start();
    }

    public static SimpleHttpServer create(int port, String path, Consumer<HttpExchange> exchangeConsumer) throws IOException {
        return new SimpleHttpServer(port, path, exchangeConsumer);
    }

    @Override
    public void close() throws Exception {
        if (delegate != null) {
            delegate.stop(0);
        }
    }

    private static class MyHandler implements HttpHandler {
        private final Consumer<HttpExchange> exchangeConsumer;

        private MyHandler(Consumer<HttpExchange> exchangeConsumer) {
            this.exchangeConsumer = Objects.requireNonNull(exchangeConsumer);
        }

        @Override
        public void handle(HttpExchange t)  {
            exchangeConsumer.accept(t);
        }
    }



}
