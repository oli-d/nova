/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageDetails;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class Endpoint<MessageType>  {
    private final EndpointStreamSource<MessageType> streamSource;
    private final Optional<Consumer<CloseReason>> closeAction;

    protected Endpoint(EndpointStreamSource<MessageType> streamSource) {
        this(streamSource, null);
    }

    protected Endpoint(EndpointStreamSource<MessageType> streamSource, Consumer<CloseReason> closeAction) {
        Objects.requireNonNull(streamSource, "streamSource must not be null");
        this.streamSource = streamSource;
        this.closeAction = Optional.ofNullable(closeAction);
    }

    public Flowable<WebSocket<MessageType>> connectedWebSockets(BackpressureStrategy backpressureStrategy) {
        return streamSource
                .connectingSockets
                .toFlowable(backpressureStrategy);
    }

    public Flowable<IncomingMessage<MessageType, String, WebSocketSpecificDetails>> messages (BackpressureStrategy backpressureStrategy) {
        return streamSource
                .messages
                .toFlowable(backpressureStrategy)
                .map(tuple -> {
                    WebSocketSpecificDetails webSocketSpecificDetails =
                            new WebSocketSpecificDetails(tuple._3);
                    IncomingMessageDetails<String, WebSocketSpecificDetails> incomingMessageDetails =
                            new IncomingMessageDetails.Builder<String, WebSocketSpecificDetails>()
                                    .withDestination(tuple._2)
                                    .withTransportSpecificDetails(webSocketSpecificDetails)
                                    .build();
                    return new IncomingMessage<>(tuple._1, incomingMessageDetails);
                });
    }

    public Flowable<Pair<WebSocket<MessageType>, Throwable>> errors (BackpressureStrategy backpressureStrategy) {
        return streamSource
            .errors
            .toFlowable(backpressureStrategy);
    }

    public Flowable<Pair<WebSocket<MessageType>, CloseReason>> closedWebSockets(BackpressureStrategy backpressureStrategy) {
        return streamSource
                .closingSockets
                .toFlowable(backpressureStrategy);
    }

    public void close () {
        close(CloseReason.NORMAL_CLOSURE);
    }

    public void close (CloseReason closeReason) {
        if (!closeReason.mightBeUsedByEndpoint) {
            throw new IllegalArgumentException("CloseReason " + closeReason + " cannot be used by endpoints");
        }
        closeAction.ifPresent(closeAction -> closeAction.accept(closeReason));
    }
}
