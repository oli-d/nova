package ch.squaredesk.nova.comm.websockets.client;

import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.Endpoint;
import ch.squaredesk.nova.comm.websockets.EndpointStreamSource;
import ch.squaredesk.nova.comm.websockets.WebSocket;

import java.util.function.Consumer;

public class ClientEndpoint<MessageType> extends Endpoint<MessageType> {
    private final ch.squaredesk.nova.comm.websockets.WebSocket<MessageType> webSocket;

    public ClientEndpoint(EndpointStreamSource<MessageType> endpointStreamSource,
                          WebSocket<MessageType> webSocket,
                          Consumer<CloseReason> closeAction) {
        super(endpointStreamSource, closeAction);
        this.webSocket = webSocket;
    }

    public void send(MessageType message) {
        webSocket.send(message);
    }
}
