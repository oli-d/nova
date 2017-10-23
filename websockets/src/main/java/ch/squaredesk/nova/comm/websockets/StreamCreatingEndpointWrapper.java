package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.Observable;

public interface StreamCreatingEndpointWrapper<WebsocketType, MessageType>  {
    Observable<Pair<WebsocketType, MessageType>> messages();
    Observable<WebsocketType> connectingSockets();
    Observable<WebsocketType> closingSockets();
    Observable<Pair<WebsocketType, Throwable>> errors();
}
