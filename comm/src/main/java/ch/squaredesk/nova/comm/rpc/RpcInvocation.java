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
import ch.squaredesk.nova.comm.sending.OutgoingMessageTranscriber;
import ch.squaredesk.nova.tuples.Pair;

import java.util.function.Consumer;

public class RpcInvocation<
        RequestType,
        IncomingMetaDataType extends IncomingMessageMetaData<?,?>,
        TransportMessageType,
        TransportSpecificReplyInfo>
    implements RpcCompletor<TransportMessageType, TransportSpecificReplyInfo> {

    public final IncomingMessage<RequestType, IncomingMetaDataType> request;

    private final Consumer<Pair<TransportMessageType, TransportSpecificReplyInfo>> replyConsumer;
    private final Consumer<Throwable> errorConsumer;

    public RpcInvocation(IncomingMessage<RequestType, IncomingMetaDataType> request,
                         Consumer<Pair<TransportMessageType, TransportSpecificReplyInfo>> replyConsumer,
                         Consumer<Throwable> errorConsumer) {
        this.request = request;
        this.replyConsumer = replyConsumer;
        this.errorConsumer = errorConsumer;
    }

    @Override
    public <T> void complete(T reply, TransportSpecificReplyInfo replySpecificInfo, OutgoingMessageTranscriber<TransportMessageType> transcriber) throws Exception {
        replyConsumer.accept(new Pair<>(transcriber.transcribeOutgoingMessage(reply), replySpecificInfo));

    }

    public void completeExceptionally(Throwable error) {
        errorConsumer.accept(error);
    }

    @Override
    public String toString() {
        return "{ request=" + request + '}';
    }
}
