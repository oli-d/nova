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
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class JmsRpcClient<InternalMessageType> extends RpcClient<Destination, InternalMessageType, JmsSpecificInfo> {
    private static final Logger logger = LoggerFactory.getLogger(JmsRpcClient.class);

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
    public <RequestType extends InternalMessageType, ReplyType extends InternalMessageType> Single<ReplyType> sendRequest(
            RequestType request,
            MessageSendingInfo<Destination, JmsSpecificInfo> messageSendingInfo,
            long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        // listen to RPC reply. This must be done BEFORE sending the request, otherwise we could miss a very fast response
        // if the Observable is hot
        Single replySingle = messageReceiver.messages(messageSendingInfo.transportSpecificInfo.replyDestination)
                .filter(incomingMessage ->
                        incomingMessage.details.transportSpecificDetails != null &&
                        messageSendingInfo.transportSpecificInfo.correlationId.equals(incomingMessage.details.transportSpecificDetails.correlationId))
                .take(1)
                .map(incomingMessage -> incomingMessage.message)
                .doOnNext(reply -> metricsCollector.rpcCompleted(request, reply))
                .single((InternalMessageType) new Object()); // TODO a bit ugly, isn't it? Is there a nicer way? But: we should never be able to run into this

        // send message sync
        Throwable sendError = messageSender.sendMessage(
                messageSendingInfo.destination, request, messageSendingInfo.transportSpecificInfo).blockingGet();
        if (sendError != null) {
            return Single.error(sendError);
        }

        // and either timeout or return RPC reply
        Single timeoutSingle = Single.create(s -> metricsCollector.rpcTimedOut(request)).timeout(timeout, timeUnit);

        return replySingle.ambWith(timeoutSingle);
    }
}
