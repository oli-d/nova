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
package ch.squaredesk.nova.comm.websockets.client;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ws.WebSocketUpgradeHandler;

public class WebSocketFactory {
    private final MessageTranscriber<String> messageTranscriber;

    public WebSocketFactory(MessageTranscriber<String> messageTranscriber) {
        this.messageTranscriber = messageTranscriber;
    }

    public WebSocket createFor (
            AsyncHttpClient httpClient,
            String destination,
            MetricsCollector metricsCollector)  {

        WebSocketUpgradeHandler webSocketUpgradeHandler = new WebSocketUpgradeHandler.Builder().build();
        com.ning.http.client.ws.WebSocket underlyingWebSocket = openConnection(httpClient, destination, webSocketUpgradeHandler);

        return new ClientSideWebSocket(destination, underlyingWebSocket, messageTranscriber, metricsCollector);
    }

    private static com.ning.http.client.ws.WebSocket openConnection(
            AsyncHttpClient httpClient,
            String destination,
            WebSocketUpgradeHandler webSocketUpgradeHandler) {
        try {
            return httpClient.prepareGet(destination).execute(webSocketUpgradeHandler).get();
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to " + destination, e);
        }
    }
}
