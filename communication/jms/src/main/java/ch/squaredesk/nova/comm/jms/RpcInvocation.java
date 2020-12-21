/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.tuples.Pair;

import java.util.function.Consumer;

public class RpcInvocation<MessageType> extends
        ch.squaredesk.nova.comm.rpc.RpcInvocation<MessageType, IncomingMessageMetaData, String, SendInfo> {

    public RpcInvocation(IncomingMessage<MessageType, IncomingMessageMetaData> request,
                         Consumer<Pair<String, SendInfo>> replyConsumer,
                         Consumer<Throwable> errorConsumer) {
        super(request, replyConsumer, errorConsumer);
    }
}
