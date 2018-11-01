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
import ch.squaredesk.nova.comm.websockets.*;

import java.util.function.Consumer;

public class ServerEndpoint extends Endpoint {
    private final SendAction broadcastAction;

    public ServerEndpoint(String destination,
                          EndpointStreamSource endpointStreamSource,
                          SendAction broadcastAction,
                          Consumer<CloseReason> closeAction,
                          MessageTranscriber<String> messageTranscriber,
                          MetricsCollector metricsCollector) {
        super(destination, endpointStreamSource, closeAction, messageTranscriber, metricsCollector);
        this.broadcastAction = broadcastAction;
    }

    public <T> void broadcast (T message)throws Exception {
        broadcastAction.accept(message);
    }
}
