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

import ch.squaredesk.nova.comm.BackpressuredStreamFromAsyncSource;
import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.StreamCreatingEndpointWrapper;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.Flowable;
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

    // TODO: do we need toSerialized versions? grizzly is nio, though...
    private final BackpressuredStreamFromAsyncSource<Pair<WebSocket, MessageType>> messages = new BackpressuredStreamFromAsyncSource<>();
    private final BackpressuredStreamFromAsyncSource<WebSocket> connectedSockets = new BackpressuredStreamFromAsyncSource<>();
    private final BackpressuredStreamFromAsyncSource<Pair<WebSocket, CloseReason>> closedSockets = new BackpressuredStreamFromAsyncSource<>();
    private final BackpressuredStreamFromAsyncSource<Pair<WebSocket, Throwable>> errors = new BackpressuredStreamFromAsyncSource<>();

    private final Function<String, MessageType> messageUnmarshaller;

    StreamCreatingWebSocketApplication(Function<String, MessageType> messageUnmarshaller) {
        this.messageUnmarshaller = messageUnmarshaller;
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        // FIXME: convert dataFrame to something useful
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
        // TODO verify: is onClose() invoked?
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        try {
            messages.onNext(new Pair<>(socket, messageUnmarshaller.apply(text)));
        } catch (Exception e) {
            // must be caught to keep the Observable functional
            logger.info("", e);
        }
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
        messages.onComplete();
        connectedSockets.onComplete();
        closedSockets.onComplete();
        errors.onComplete();
    }

}
