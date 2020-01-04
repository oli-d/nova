/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.rpc;

import io.reactivex.functions.Function;

public interface RpcCompletor<TransportMessageType, TransportSpecificReplyInfo> {
    default <T> void complete(T reply, Function<T, TransportMessageType> messageTranscriber) throws Exception {
        complete(reply, null, messageTranscriber);
    }

    <T> void  complete(T reply, TransportSpecificReplyInfo replySpecificInfo, Function<T, TransportMessageType> transcriber) throws Exception;

    void completeExceptionally(Throwable error);
}
