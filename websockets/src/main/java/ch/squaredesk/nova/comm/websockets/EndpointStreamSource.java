package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.tuples.Pair;
import ch.squaredesk.nova.tuples.Tuple3;
import io.reactivex.Observable;

public class EndpointStreamSource<MessageType>  {
    public final Observable<Tuple3<MessageType, String, WebSocket>> messages;
    public final Observable<WebSocket> connectingSockets;
    public final Observable<Pair<WebSocket, Object>> closingSockets;
    public final Observable<Pair<WebSocket, Throwable>> errors;


    public EndpointStreamSource(Observable<Tuple3<MessageType, String, WebSocket>> messages,
                                Observable<WebSocket> connectingSockets,
                                Observable<Pair<WebSocket, Object>> closingSockets,
                                Observable<Pair<WebSocket, Throwable>> errors) {
        this.messages = messages;
        this.connectingSockets = connectingSockets;
        this.closingSockets = closingSockets;
        this.errors = errors;
    }
}
