/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets.server;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import ch.squaredesk.nova.comm.websockets.WebSocket;

public class ServerEndpointFactory {
    public WebSocket createFor(
            String destination,
            org.glassfish.grizzly.websockets.WebSocket underlyingWebSocket,
            MessageTranscriber<String> messageTranscriber,
            MetricsCollector metricsCollector)  {

        return new ServerSideWebSocket(destination, underlyingWebSocket, messageTranscriber, metricsCollector);
    }
}
