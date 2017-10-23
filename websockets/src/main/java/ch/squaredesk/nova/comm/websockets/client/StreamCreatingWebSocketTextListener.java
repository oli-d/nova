package ch.squaredesk.nova.comm.websockets.client;

import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.StreamCreatingEndpointWrapper;
import ch.squaredesk.nova.tuples.Pair;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class StreamCreatingWebSocketTextListener<MessageType>
        implements WebSocketTextListener, StreamCreatingEndpointWrapper<WebSocket, MessageType> {

    private static final Logger logger = LoggerFactory.getLogger(StreamCreatingEndpointWrapper.class);

    // TODO: do we need toSerialized versions? grizzly is nio, though...
    private final Subject<Pair<WebSocket, MessageType>> messageSubject = PublishSubject.create();
    private final Subject<WebSocket> connectionSubject = BehaviorSubject.create();
    private final Subject<Pair<WebSocket, CloseReason>> closeSubject = PublishSubject.create();
    private final Subject<Pair<WebSocket, Throwable>> errorSubject = PublishSubject.create();

    private final Function<String, MessageType> messageUnmarshaller;

    public StreamCreatingWebSocketTextListener(Function<String, MessageType> messageUnmarshaller) {
        this.messageUnmarshaller = messageUnmarshaller;
    }

    @Override
    public void onMessage(String messageText) {
        try {
            messageSubject.onNext(new Pair<>(null, messageUnmarshaller.apply(messageText)));
        } catch (Exception e) {
            // must be caught to keep the Observable functional
            logger.info("Unable to unmarshal incoming message " + messageText, e);
        }
    }

    @Override
    public void onOpen(WebSocket websocket) {
        connectionSubject.onNext(websocket);
    }

    @Override
    public void onClose(WebSocket websocket) {
        // FIXME: close reason
        closeSubject.onNext(new Pair<>(websocket, CloseReason.NO_STATUS_CODE));
    }

    @Override
    public void onError(Throwable t) {
        errorSubject.onNext(new Pair<>(null, t));
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
        errorSubject.onComplete();
        // FIXME closeSubject.onComplete();
    }
}
