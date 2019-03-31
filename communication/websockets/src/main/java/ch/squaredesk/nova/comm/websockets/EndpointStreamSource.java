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

public class EndpointStreamSource  {
    final Flowable<Tuple3<String, String, WebSocket>> messages;
    final Flowable<WebSocket> connectingSockets;
    final Flowable<Pair<WebSocket, CloseReason>> closingSockets;
    final Flowable<Pair<WebSocket, Throwable>> errors;


    EndpointStreamSource(Flowable<Tuple3<String, String, WebSocket>> messages,
                         Flowable<WebSocket> connectingSockets,
                         Flowable<Pair<WebSocket, CloseReason>> closingSockets,
                         Flowable<Pair<WebSocket, Throwable>> errors) {
        this.messages = messages;
        this.connectingSockets = connectingSockets;
        this.closingSockets = closingSockets;
        this.errors = errors;
    }

}
