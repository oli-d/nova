/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets.client;

import ch.squaredesk.nova.comm.BackpressuredStreamFromAsyncSource;
import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.StreamCreatingEndpointWrapper;
import ch.squaredesk.nova.tuples.Pair;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class StreamCreatingWebSocketTextListener<MessageType>
        implements WebSocketTextListener, StreamCreatingEndpointWrapper<WebSocket, MessageType> {

    private static final Logger logger = LoggerFactory.getLogger(StreamCreatingEndpointWrapper.class);

    private final BackpressuredStreamFromAsyncSource<Pair<WebSocket, MessageType>> messages = new BackpressuredStreamFromAsyncSource<>();
    private final BackpressuredStreamFromAsyncSource<WebSocket> connectedSockets = new BackpressuredStreamFromAsyncSource<>();
    private final BackpressuredStreamFromAsyncSource<Pair<WebSocket, CloseReason>> closedSockets = new BackpressuredStreamFromAsyncSource<>();
    private final BackpressuredStreamFromAsyncSource<Pair<WebSocket, Throwable>> errors = new BackpressuredStreamFromAsyncSource<>();

    private final Function<String, MessageType> messageUnmarshaller;

    StreamCreatingWebSocketTextListener(Function<String, MessageType> messageUnmarshaller) {
        this.messageUnmarshaller = messageUnmarshaller;
    }

    @Override
    public void onMessage(String messageText) {
        try {
            messages.onNext(new Pair<>(null, messageUnmarshaller.apply(messageText)));
        } catch (Exception e) {
            // must be caught to keep the Observable functional
            logger.info("Unable to unmarshal incoming message " + messageText, e);
        }
    }

    @Override
    public void onOpen(WebSocket websocket) {
        connectedSockets.onNext(websocket);
    }

    @Override
    public void onClose(WebSocket websocket) {
        // FIXME: close reason
        closedSockets.onNext(new Pair<>(websocket, CloseReason.NO_STATUS_CODE));
    }

    @Override
    public void onError(Throwable t) {
        errors.onNext(new Pair<>(null, t));
    }

    @Override
    public Flowable<Pair<WebSocket, MessageType>> messages() {
        return messages.toFlowable();
    }

    @Override
    public Flowable<WebSocket> connectingSockets() {
        return connectedSockets.toFlowable();
    }

    @Override
    public Flowable<Pair<WebSocket, CloseReason>> closingSockets() {
        return closedSockets.toFlowable();
    }

    @Override
    public Flowable<Pair<WebSocket, Throwable>> errors() {
        return errors.toFlowable();
    }

    void close() {
        closedSockets.onComplete();
        connectedSockets.onComplete();
        errors.onComplete();
        // FIXME closeSubject.onComplete();
    }
}
