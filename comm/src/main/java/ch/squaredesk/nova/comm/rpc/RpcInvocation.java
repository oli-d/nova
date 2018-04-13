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

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.tuples.Pair;

import java.util.function.Consumer;

public class RpcInvocation<
        RequestType,
        IncomingMetaDataType extends IncomingMessageMetaData<?,?>,
        ReplyType,
        TransportSpecificReplyInfo>
    implements RpcCompletor<ReplyType, TransportSpecificReplyInfo> {

    public final IncomingMessage<RequestType, IncomingMetaDataType> request;

    private final Consumer<Pair<ReplyType, TransportSpecificReplyInfo>> replyConsumer;
    private final Consumer<Throwable> errorConsumer;

    public RpcInvocation(IncomingMessage<RequestType, IncomingMetaDataType> request,
                         Consumer<Pair<ReplyType, TransportSpecificReplyInfo>> replyConsumer,
                         Consumer<Throwable> errorConsumer) {
        this.request = request;
        this.replyConsumer = replyConsumer;
        this.errorConsumer = errorConsumer;
    }

    public void complete(ReplyType reply, TransportSpecificReplyInfo replySpecificInfo) {
        replyConsumer.accept(new Pair<>(reply, replySpecificInfo));
    }

    public void completeExceptionally(Throwable error) {
        errorConsumer.accept(error);
    }

    @Override
    public String toString() {
        return "{ request=" + request + '}';
    }
}
