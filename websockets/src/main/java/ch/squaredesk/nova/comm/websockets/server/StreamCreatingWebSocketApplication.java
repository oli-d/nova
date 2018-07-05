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

import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.StreamCreatingEndpointWrapper;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.glassfish.grizzly.websockets.ClosingFrame;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class StreamCreatingWebSocketApplication<MessageType>
        extends WebSocketApplication
        implements StreamCreatingEndpointWrapper<WebSocket, MessageType> {

    private static final Logger logger = LoggerFactory.getLogger(StreamCreatingWebSocketApplication.class);

    private final Subject<Pair<WebSocket, MessageType>> messages = PublishSubject.<Pair<WebSocket, MessageType>>create().toSerialized();
    private final Subject<WebSocket> connectedSockets = PublishSubject.create();
    private final Subject<Pair<WebSocket, CloseReason>> closedSockets = PublishSubject.create();
    private final Subject<Pair<WebSocket, Throwable>> errors = PublishSubject.create();

    private final Function<String, MessageType> messageUnmarshaller;

    StreamCreatingWebSocketApplication(Function<String, MessageType> messageUnmarshaller) {
        this.messageUnmarshaller = messageUnmarshaller;
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        ClosingFrame closingFrame = (ClosingFrame)frame;
        CloseReason closeReason;
        try {
            closeReason = CloseReason.forCloseCode(closingFrame.getCode());
        } catch (Exception e) {
            logger.error("Unexpected close code " + closingFrame.getCode() + " in closing dataFrame " + frame);
            closeReason = CloseReason.UNEXPECTED_CONDITION;
        }
        closedSockets.onNext(new Pair<>(socket, closeReason));
    }

    @Override
    public void onConnect(WebSocket socket) {
        connectedSockets.onNext(socket);
    }

    @Override
    protected boolean onError(WebSocket socket, Throwable t) {
        errors.onNext(new Pair<>(socket, t));
        return true; // close webSocket
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        try {
            messages.onNext(new Pair<>(socket, messageUnmarshaller.apply(text)));
            /**
             * One comment regarding backpressure: if the stream subscribers are too slow,
             * backpressure will be applied here. So if e.g. the subscriber need one hour
             * to process a message, the next onNext() call will be blocked for that hour.
             * But be aware that the next message has already been read from the wire into
             * memory, so if the process is killed at this point in time the message that
             * was not yet read from the wire is lost.
             * TL;DR: backpressure is applied, but it does not prevent from loss of messages
             */
        } catch (Exception e) {
            // must be caught to keep the Observable functional
            logger.info("", e);
        }
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
        closedSockets.onComplete();
        errors.onComplete();
    }

}
