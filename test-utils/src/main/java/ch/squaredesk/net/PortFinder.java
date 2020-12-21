/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.net;

import java.net.ServerSocket;
import java.util.function.IntConsumer;

public class PortFinder {
    private static final Object portLock = new Object();

    private PortFinder() {
    }

    public static void withNextFreePort (IntConsumer consumer) {
        synchronized (portLock) {
            int port = findFreePort();
            consumer.accept(port);
        }
    }

    public static final int findFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
