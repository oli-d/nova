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

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.comm.rpc.RpcServer;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;

import javax.jms.Destination;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class JmsRpcServer<InternalMessageType> extends RpcServer<Destination, InternalMessageType, JmsSpecificInfo> {

    private final JmsMessageSender<InternalMessageType> messageSender;
    private final JmsMessageReceiver<InternalMessageType> messageReceiver;
    private final Function<Throwable, InternalMessageType> errorReplyFactory;

    JmsRpcServer(String identifier,
                        JmsMessageReceiver<InternalMessageType> messageReceiver,
                        JmsMessageSender<InternalMessageType> messageSender,
                        Function<Throwable, InternalMessageType> errorReplyFactory,
                        Metrics metrics) {
        super(identifier, metrics);

        requireNonNull(messageSender, "messageSender must not be null");
        requireNonNull(messageReceiver, "messageReceiver must not be null");
        requireNonNull(errorReplyFactory, "errorReplyFactory must not be null");
        this.messageSender = messageSender;
        this.messageReceiver = messageReceiver;
        this.errorReplyFactory = errorReplyFactory;
    }

    @Override
    public <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
        Flowable<RpcInvocation<RequestType, ReplyType, JmsSpecificInfo>> requests(Destination destination) {
        return messageReceiver.messages(destination)
                .filter(this::isRpcRequest)
                .map(incomingMessage -> {
                    metricsCollector.requestReceived(incomingMessage.message);
                    RequestType request = (RequestType) incomingMessage.message;
                    Consumer<ReplyType> replyConsumer = createReplyHandlerFor(incomingMessage);
                    Consumer<Throwable> errorConsumer = createErrorReplyHandlerFor(incomingMessage);
                    return new RpcInvocation<>(
                            request,
                            incomingMessage.details.transportSpecificDetails,
                            reply -> {
                                replyConsumer.accept(reply);
                                metricsCollector.requestCompleted(incomingMessage.message, reply);
                            },
                            error -> {
                                metricsCollector.requestCompletedExceptionally(incomingMessage.message, error);
                                errorConsumer.accept(error);
                            });
                });
    }

    private boolean isRpcRequest(IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo> incomingMessage) {
        return incomingMessage.details != null &&
                incomingMessage.details.transportSpecificDetails != null &&
                incomingMessage.details.transportSpecificDetails.replyDestination != null &&
                incomingMessage.details.transportSpecificDetails.correlationId != null;
    }

    private <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
        Consumer<ReplyType> createReplyHandlerFor(IncomingMessage<RequestType, Destination, JmsSpecificInfo> request) {
        JmsSpecificInfo sendingInfo = new JmsSpecificInfo(
                request.details.transportSpecificDetails.correlationId,
                null,
                null,
                null,
                null,
                null);
        return reply -> messageSender.sendMessage(
                request.details.transportSpecificDetails.replyDestination,
                reply,
                sendingInfo); // FIXME: bug: missing subscribe.
    }

    private Consumer<Throwable> createErrorReplyHandlerFor(IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo> request) {
        JmsSpecificInfo sendingInfo = new JmsSpecificInfo(
                request.details.transportSpecificDetails.correlationId,
                null,
                null,
                null,
                null,
                null);
        return error -> messageSender.sendMessage(
                request.details.transportSpecificDetails.replyDestination,
                errorReplyFactory.apply(error),
                sendingInfo);
    }


}
