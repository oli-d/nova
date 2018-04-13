/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

public class RpcClient<InternalMessageType> extends ch.squaredesk.nova.comm.rpc.RpcClient<InternalMessageType, OutgoingMessageMetaData, IncomingMessageMetaData> {
    private final MessageSender<InternalMessageType> messageSender;
    private final MessageReceiver<InternalMessageType> messageReceiver;

    public RpcClient(String identifier,
                     MessageReceiver<InternalMessageType> messageReceiver,
                     MessageSender<InternalMessageType> messageSender,
                     Metrics metrics) {
        super(identifier, metrics);
        this.messageSender = messageSender;
        this.messageReceiver = messageReceiver;
    }

    @Override
    public <ReplyType extends InternalMessageType> Single<RpcReply<ReplyType>> sendRequest(
            InternalMessageType request,
            OutgoingMessageMetaData outgoingMessageMetaData,
            long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");
        requireNonNull(outgoingMessageMetaData, "metaData must not be null");
        requireNonNull(outgoingMessageMetaData.details, "metaData.details must not be null");
        requireNonNull(outgoingMessageMetaData.details.correlationId, "correlationId must not be null");
        requireNonNull(outgoingMessageMetaData.details.replyDestination, "replyDestination must not be null");

        // listen to RPC reply. This must be done BEFORE sending the request, otherwise we could miss a very fast response
        // if the Observable is hot
        Single<RpcReply<ReplyType>> replySingle =
                messageReceiver.messages(outgoingMessageMetaData.details.replyDestination)
                .filter(incomingMessage ->
                        incomingMessage.metaData.details != null &&
                        outgoingMessageMetaData.details.correlationId.equals(incomingMessage.metaData.details.correlationId))
                .take(1)
                .doOnNext(reply -> metricsCollector.rpcCompleted(request, reply))
                .map(incomingMessage -> new RpcReply<>((ReplyType)incomingMessage.message, incomingMessage.metaData))
                .single(new RpcReply<>(null, null)); // TODO a bit ugly, isn't it? Is there a nicer way? But: we should never be able to run into this

        // send message sync
        Throwable sendError = messageSender.doSend(request, outgoingMessageMetaData).blockingGet();
        if (sendError != null) {
            return Single.error(sendError);
        }

        return replySingle
                .timeout(timeout, timeUnit)
                .doOnError(t -> {
                    if (t instanceof TimeoutException) {
                        metricsCollector.rpcTimedOut(request);
                    }
                });
    }

}
