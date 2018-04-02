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

import ch.squaredesk.nova.comm.rpc.RpcClient;
import ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;

import javax.jms.Destination;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

public class JmsRpcClient<InternalMessageType> extends RpcClient<Destination, InternalMessageType, JmsSpecificInfo, JmsSpecificInfo> {
    private final JmsMessageSender<InternalMessageType> messageSender;
    private final JmsMessageReceiver<InternalMessageType> messageReceiver;

    public JmsRpcClient(String identifier,
                        JmsMessageReceiver<InternalMessageType> messageReceiver,
                        JmsMessageSender<InternalMessageType> messageSender,
                        Metrics metrics) {
        super(identifier, metrics);
        this.messageSender = messageSender;
        this.messageReceiver = messageReceiver;
    }

    @Override
    public <ReplyType extends InternalMessageType> Single<JmsRpcReply<ReplyType>> sendRequest(
            InternalMessageType request,
            OutgoingMessageMetaData<Destination, JmsSpecificInfo> outgoingMessageMetaData,
            long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        // listen to RPC reply. This must be done BEFORE sending the request, otherwise we could miss a very fast response
        // if the Observable is hot
        Single<JmsRpcReply<ReplyType>> replySingle =
                messageReceiver.messages(outgoingMessageMetaData.transportSpecificInfo.replyDestination)
                .filter(incomingMessage ->
                        incomingMessage.details.transportSpecificDetails != null &&
                        outgoingMessageMetaData.transportSpecificInfo.correlationId.equals(incomingMessage.details.transportSpecificDetails.correlationId))
                .take(1)
                .doOnNext(reply -> metricsCollector.rpcCompleted(request, reply))
                .map(incomingMessage -> new JmsRpcReply<>((ReplyType)incomingMessage.message, incomingMessage.details))
                .single(new JmsRpcReply<>(null, null)); // TODO a bit ugly, isn't it? Is there a nicer way? But: we should never be able to run into this

        // send message sync
        Throwable sendError = messageSender.sendMessage(
                outgoingMessageMetaData.destination, request, outgoingMessageMetaData.transportSpecificInfo).blockingGet();
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
