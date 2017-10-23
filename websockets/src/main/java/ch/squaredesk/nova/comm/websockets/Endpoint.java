package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageDetails;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

public class Endpoint<MessageType>  {
    private final EndpointStreamSource<MessageType> streamSource;

    protected Endpoint(EndpointStreamSource<MessageType> streamSource) {
        this.streamSource = streamSource;
    }

    public Flowable<WebSocket> connectedWebSockets(BackpressureStrategy backpressureStrategy) {
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

    public Flowable<Pair<WebSocket, Throwable>> errors (BackpressureStrategy backpressureStrategy) {
        return streamSource
            .errors
            .toFlowable(backpressureStrategy);
    }

    public Flowable<Pair<WebSocket, Object>> closedWebSockets(BackpressureStrategy backpressureStrategy) {
        return streamSource
                .closingSockets
                .toFlowable(backpressureStrategy);
    }
}
