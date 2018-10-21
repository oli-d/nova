/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.comm.sending.MessageMarshaller;

public interface RpcCompletor<TransportMessageType, TransportSpecificReplyInfo> {
    default <T> void complete(T reply, MessageMarshaller<T, TransportMessageType> messageMarshaller) throws Exception {
        complete(reply, messageMarshaller, null);
    }

    <T> void  complete(T reply, MessageMarshaller<T, TransportMessageType> messageMarshaller, TransportSpecificReplyInfo replySpecificInfo) throws Exception;

    void completeExceptionally(Throwable error);
}
