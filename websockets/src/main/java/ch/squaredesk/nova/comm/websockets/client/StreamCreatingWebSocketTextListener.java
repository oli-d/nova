package ch.squaredesk.nova.comm.websockets.client;

import ch.squaredesk.nova.comm.websockets.StreamCreatingEndpointWrapper;
import ch.squaredesk.nova.tuples.Pair;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.function.Function;

public class StreamCreatingWebSocketTextListener<MessageType>
        implements WebSocketTextListener, StreamCreatingEndpointWrapper<WebSocket, MessageType> {
    // TODO: do we need toSerialized versions? grizzly is nio, though...
    private final Subject<Pair<WebSocket, MessageType>> messageSubject = PublishSubject.create();
    private final Subject<WebSocket> connectionSubject = PublishSubject.create();
    private final Subject<WebSocket> closeSubject = PublishSubject.create();
    private final Subject<Pair<WebSocket, Throwable>> errorSubject = PublishSubject.create();

    private final Function<String, MessageType> messageUnmarshaller;

    public StreamCreatingWebSocketTextListener(Function<String, MessageType> messageUnmarshaller) {
        this.messageUnmarshaller = messageUnmarshaller;
    }

    @Override
    public void onMessage(String messageText) {
        messageSubject.onNext(new Pair<>(null, messageUnmarshaller.apply(messageText)));
    }

    @Override
    public void onOpen(WebSocket websocket) {
        connectionSubject.onNext(websocket);
    }

    @Override
    public void onClose(WebSocket websocket) {
        // FIXME: close reason
        closeSubject.onNext(websocket);
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
    public Observable<WebSocket> closingSockets() {
        return closeSubject;
    }

    @Override
    public Observable<Pair<WebSocket, Throwable>> errors() {
        return errorSubject;
    }
}
