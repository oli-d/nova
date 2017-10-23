package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.tuples.Pair;
import ch.squaredesk.nova.tuples.Tuple3;
import io.reactivex.Observable;

public class EndpointStreamSource<MessageType>  {
    final Observable<Tuple3<MessageType, String, WebSocket<MessageType>>> messages;
    final Observable<WebSocket<MessageType>> connectingSockets;
    final Observable<Pair<WebSocket<MessageType>, CloseReason>> closingSockets;
    final Observable<Pair<WebSocket<MessageType>, Throwable>> errors;


    EndpointStreamSource(Observable<Tuple3<MessageType, String, WebSocket<MessageType>>> messages,
                                Observable<WebSocket<MessageType>> connectingSockets,
                                Observable<Pair<WebSocket<MessageType>, CloseReason>> closingSockets,
                                Observable<Pair<WebSocket<MessageType>, Throwable>> errors) {
        this.messages = messages;
        this.connectingSockets = connectingSockets;
        this.closingSockets = closingSockets;
        this.errors = errors;
    }

}
