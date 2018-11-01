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
import com.ning.http.client.ws.WebSocketCloseCodeReasonListener;
import com.ning.http.client.ws.WebSocketTextListener;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamCreatingWebSocketTextListener
        implements WebSocketTextListener, WebSocketCloseCodeReasonListener, StreamCreatingEndpointWrapper<WebSocket> {

    private static final Logger logger = LoggerFactory.getLogger(StreamCreatingEndpointWrapper.class);

    private final Subject<Pair<WebSocket, String>> messages = PublishSubject.create();
    private final Subject<WebSocket> connectedSockets = BehaviorSubject.create();
    private final Subject<Pair<WebSocket, CloseReason>> closedSockets = PublishSubject.create();
    private final Subject<Pair<WebSocket, Throwable>> errors = PublishSubject.create();

    StreamCreatingWebSocketTextListener() {
    }

    @Override
    public void onMessage(String messageText) {
        messages.onNext(new Pair<>(null, messageText));
    }

    @Override
    public void onOpen(WebSocket websocket) {
        connectedSockets.onNext(websocket);
    }

    @Override
    public void onClose(WebSocket websocket) {
        // noop, this is handled in the specific onClose(WebSocket, code, reason)
    }

    @Override
    public void onClose(WebSocket websocket, int code, String reason) {
        logger.trace("onClose() invoked with webSocket={}, code={}, reason={}.", websocket, code, reason);
        CloseReason closeReason;
        try {
            closeReason = CloseReason.forCloseCode(code);
        } catch (Exception e) {
            closeReason = CloseReason.NO_STATUS_CODE;
        }
        closedSockets.onNext(new Pair<>(websocket, closeReason));
    }

    @Override
    public void onError(Throwable t) {
        errors.onNext(new Pair<>(null, t));
    }

    @Override
    public Flowable<Pair<WebSocket, String>> messages() {
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
