/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.rxjava3.core.Flowable;

public interface StreamCreatingEndpointWrapper<WebsocketType>  {
    Flowable<Pair<WebsocketType, String>> messages();
    Flowable<WebsocketType> connectingSockets();
    Flowable<Pair<WebsocketType, CloseReason>> closingSockets();
    Flowable<Pair<WebsocketType, Throwable>> errors();
}
