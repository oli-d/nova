/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.net;

import java.net.ServerSocket;
import java.util.function.Consumer;

public class PortFinder {
    private static final Object portLock = new Object();

    private PortFinder() {
    }

    public static void withNextFreePort (Consumer<Integer> consumer) {
        synchronized (portLock) {
            int port;
            try(ServerSocket serverSocket = new ServerSocket(0)) {
                port = serverSocket.getLocalPort();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            consumer.accept(port);
        }
    }
}
