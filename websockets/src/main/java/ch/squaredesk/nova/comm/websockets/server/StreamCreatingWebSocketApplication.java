package ch.squaredesk.nova.comm.websockets.server;

import ch.squaredesk.nova.comm.websockets.StreamCreatingEndpointWrapper;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;

import java.util.function.Function;

public class StreamCreatingWebSocketApplication<MessageType>
        extends WebSocketApplication
        implements StreamCreatingEndpointWrapper<WebSocket,MessageType> {

    // TODO: do we need toSerialized versions? grizzly is nio, though...
    private final Subject<Pair<WebSocket, MessageType>> messageSubject = PublishSubject.create();
    private final Subject<WebSocket> connectionSubject = PublishSubject.create();
    private final Subject<WebSocket> closeSubject = PublishSubject.create();
    private final Subject<Pair<WebSocket, Throwable>> errorSubject = PublishSubject.create();

    private final Function<String, MessageType> messageUnmarshaller;

    public StreamCreatingWebSocketApplication(Function<String, MessageType> messageUnmarshaller) {
        this.messageUnmarshaller = messageUnmarshaller;
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        // FIXME: convert dataFrame to something useful
        closeSubject.onNext(socket);
    }

    @Override
    public void onConnect(WebSocket socket) {
        connectionSubject.onNext(socket);
    }

    @Override
    protected boolean onError(WebSocket socket, Throwable t) {
        errorSubject.onNext(new Pair<>(socket, t));
        // TODO verify: is onClose() invoked?
        return true; // close webSocket
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        messageSubject.onNext(new Pair<>(null, messageUnmarshaller.apply(text)));
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
