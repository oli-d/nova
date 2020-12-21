/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import io.reactivex.rxjava3.functions.Function;

public interface RpcCompletor<TransportMessageType, TransportSpecificReplyInfo> {
    default <T> void complete(T reply, Function<T, TransportMessageType> messageTranscriber) throws Throwable {
        complete(reply, null, messageTranscriber);
    }

    <T> void  complete(T reply, TransportSpecificReplyInfo replySpecificInfo, Function<T, TransportMessageType> transcriber) throws Throwable;

    void completeExceptionally(Throwable error);
}
