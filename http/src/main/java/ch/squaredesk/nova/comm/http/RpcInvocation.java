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

import ch.squaredesk.nova.tuples.Pair;

import java.util.function.Consumer;

public class RpcInvocation<InternalMessageType>
        extends ch.squaredesk.nova.comm.rpc.RpcInvocation<InternalMessageType, SendingInfo, InternalMessageType, RetrievalInfo> {

    RpcInvocation(InternalMessageType request,
                         SendingInfo transportSpecificInfo,
                         Consumer<Pair<InternalMessageType, RetrievalInfo>> replyConsumer,
                         Consumer<Throwable> errorConsumer) {
        super(request, transportSpecificInfo, replyConsumer, errorConsumer);
    }

    public void complete(int statusCode, InternalMessageType reply) {
        RetrievalInfo replyInfo = new RetrievalInfo(statusCode);
        complete(reply, replyInfo);
    }
}
