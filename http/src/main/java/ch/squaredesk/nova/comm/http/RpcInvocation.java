/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.tuples.Pair;

import java.util.function.Consumer;

public class RpcInvocation<InternalMessageType>
        extends ch.squaredesk.nova.comm.rpc.RpcInvocation<InternalMessageType, RequestMessageMetaData, InternalMessageType, ReplyInfo>
        implements RpcCompletor<InternalMessageType> {

    protected RpcInvocation(IncomingMessage<InternalMessageType,RequestMessageMetaData> request,
                            Consumer<Pair<InternalMessageType, ReplyInfo>> replyConsumer,
                            Consumer<Throwable> errorConsumer) {
        super(request, replyConsumer, errorConsumer);
    }

    public void complete(int statusCode, InternalMessageType reply) {
        ReplyInfo replyInfo = new ReplyInfo(statusCode);
        complete(reply, replyInfo);
    }
}
