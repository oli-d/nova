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
import io.reactivex.Observable;

public interface StreamCreatingEndpointWrapper<WebsocketType, MessageType>  {
    Observable<Pair<WebsocketType, MessageType>> messages();
    Observable<WebsocketType> connectingSockets();
    Observable<Pair<WebsocketType, CloseReason>> closingSockets();
    Observable<Pair<WebsocketType, Throwable>> errors();
}
