package ch.squaredesk.nova.comm.websockets.server;

import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.Endpoint;
import ch.squaredesk.nova.comm.websockets.EndpointStreamSource;

import java.util.function.Consumer;

public class ServerEndpoint<MessageType> extends Endpoint<MessageType> {
    private final Consumer<MessageType> broadcastAction;

    public ServerEndpoint(EndpointStreamSource<MessageType> endpointStreamSource,
                          Consumer<MessageType> broadcastAction,
                          Consumer<CloseReason> closeAction) {
        super(endpointStreamSource, closeAction);
        this.broadcastAction = broadcastAction;
    }

    public void broadcast (MessageType message) {
        broadcastAction.accept(message);
    }
}
