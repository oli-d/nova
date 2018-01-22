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

import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.StreamCreatingEndpointWrapper;
import ch.squaredesk.nova.tuples.Pair;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class StreamCreatingWebSocketTextListener<MessageType>
        implements WebSocketTextListener, StreamCreatingEndpointWrapper<WebSocket, MessageType> {

    private static final Logger logger = LoggerFactory.getLogger(StreamCreatingEndpointWrapper.class);

    private final Subject<Pair<WebSocket, MessageType>> messages = PublishSubject.create();
    private final Subject<WebSocket> connectedSockets = BehaviorSubject.create();
    private final Subject<Pair<WebSocket, CloseReason>> closedSockets = PublishSubject.create();
    private final Subject<Pair<WebSocket, Throwable>> errors = PublishSubject.create();

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
        return messages.toFlowable(BackpressureStrategy.BUFFER);
    }

    @Override
    public Flowable<WebSocket> connectingSockets() {
        return connectedSockets.toFlowable(BackpressureStrategy.BUFFER);
    }

    @Override
    public Flowable<Pair<WebSocket, CloseReason>> closingSockets() {
        return closedSockets.toFlowable(BackpressureStrategy.BUFFER);
    }

    @Override
    public Flowable<Pair<WebSocket, Throwable>> errors() {
        return errors.toFlowable(BackpressureStrategy.BUFFER);
    }

    void close() {
        messages.onComplete();
        connectedSockets.onComplete();
        errors.onComplete();
        // FIXME: if we call this, Flowable will be closed before we could inform eventual subscriber... closedSockets.onComplete();
    }
}
