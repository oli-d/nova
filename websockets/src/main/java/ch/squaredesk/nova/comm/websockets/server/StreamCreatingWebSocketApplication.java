package ch.squaredesk.nova.comm.websockets.server;

import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.StreamCreatingEndpointWrapper;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.Observable;
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

    // TODO: do we need toSerialized versions? grizzly is nio, though...
    private final Subject<Pair<WebSocket, MessageType>> messageSubject = PublishSubject.create();
    private final Subject<WebSocket> connectionSubject = PublishSubject.create();
    private final Subject<Pair<WebSocket, CloseReason>> closeSubject = PublishSubject.create();
    private final Subject<Pair<WebSocket, Throwable>> errorSubject = PublishSubject.create();

    private final Function<String, MessageType> messageUnmarshaller;

    public StreamCreatingWebSocketApplication(Function<String, MessageType> messageUnmarshaller) {
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
        closeSubject.onNext(new Pair<>(socket, closeReason));
    }

    @Override
    public void onConnect(WebSocket socket) {
        connectionSubject.onNext(socket);
    }

    @Override
    protected boolean onError(WebSocket socket, Throwable t) {
        errorSubject.onNext(new Pair<>(socket, t));
        return true; // close webSocket
        // TODO verify: is onClose() invoked?
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        try {
            messageSubject.onNext(new Pair<>(socket, messageUnmarshaller.apply(text)));
        } catch (Exception e) {
            // must be caught to keep the Observable functional
            logger.info("", e);
        }
    }

    @Override
    public Observable<Pair<WebSocket, MessageType>> messages() {
        return messageSubject;
    }

    @Override
    public Observable<WebSocket> connectingSockets() {
        return connectionSubject;
    }

    @Override
    public Observable<Pair<WebSocket, CloseReason>> closingSockets() {
        return closeSubject;
    }

    @Override
    public Observable<Pair<WebSocket, Throwable>> errors() {
        return errorSubject;
    }

    public void close() {
        messageSubject.onComplete();
        connectionSubject.onComplete();
        closeSubject.onComplete();
        errorSubject.onComplete();
    }

}
