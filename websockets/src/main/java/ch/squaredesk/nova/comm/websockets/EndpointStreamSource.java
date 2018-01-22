/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.tuples.Pair;
import ch.squaredesk.nova.tuples.Tuple3;
import io.reactivex.Flowable;

public class EndpointStreamSource<MessageType>  {
    final Flowable<Tuple3<MessageType, String, WebSocket<MessageType>>> messages;
    final Flowable<WebSocket<MessageType>> connectingSockets;
    final Flowable<Pair<WebSocket<MessageType>, CloseReason>> closingSockets;
    final Flowable<Pair<WebSocket<MessageType>, Throwable>> errors;


    EndpointStreamSource(Flowable<Tuple3<MessageType, String, WebSocket<MessageType>>> messages,
                         Flowable<WebSocket<MessageType>> connectingSockets,
                         Flowable<Pair<WebSocket<MessageType>, CloseReason>> closingSockets,
                         Flowable<Pair<WebSocket<MessageType>, Throwable>> errors) {
        this.messages = messages;
        this.connectingSockets = connectingSockets;
        this.closingSockets = closingSockets;
        this.errors = errors;
    }

}
