package ch.squaredesk.nova.comm.websockets.client;

import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.comm.websockets.Endpoint;
import ch.squaredesk.nova.comm.websockets.EndpointStreamSource;

public class ClientEndpoint<MessageType> extends Endpoint<MessageType> {
    private final ch.squaredesk.nova.comm.websockets.WebSocket<MessageType> webSocket;

    public ClientEndpoint(EndpointStreamSource<MessageType> endpointStreamSource,
                          WebSocket<MessageType> webSocket,
                          Runnable closeAction) {
        super(endpointStreamSource, closeAction);
        this.webSocket = webSocket;
    }

    public void send(MessageType message) {
        webSocket.send(message);
    }
}
