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
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;

import javax.jms.Destination;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class RpcServer<InternalMessageType> extends ch.squaredesk.nova.comm.rpc.RpcServer<Destination, RpcInvocation<InternalMessageType>> {

    private final MessageSender<InternalMessageType> messageSender;
    private final MessageReceiver<InternalMessageType> messageReceiver;
    private final Function<Throwable, InternalMessageType> errorReplyFactory;

    RpcServer(String identifier,
              MessageReceiver<InternalMessageType> messageReceiver,
              MessageSender<InternalMessageType> messageSender,
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
    public Flowable<RpcInvocation<InternalMessageType>> requests(Destination destination) {
        return messageReceiver.messages(destination)
                .filter(this::isRpcRequest)
                .map(incomingMessage -> {
                    metricsCollector.requestReceived(incomingMessage.message);
                    Consumer<InternalMessageType> replyConsumer = createReplyHandlerFor(incomingMessage);
                    Consumer<Throwable> errorConsumer = createErrorReplyHandlerFor(incomingMessage);
                    return new RpcInvocation<>(
                            incomingMessage,
                            reply -> {
                                replyConsumer.accept(reply._1);
                                metricsCollector.requestCompleted(incomingMessage.message, reply);
                            },
                            error -> {
                                metricsCollector.requestCompletedExceptionally(incomingMessage.message, error);
                                errorConsumer.accept(error);
                            });
                });
    }

    private boolean isRpcRequest(IncomingMessage<InternalMessageType, IncomingMessageMetaData> incomingMessage) {
        return incomingMessage.metaData != null &&
                incomingMessage.metaData.details != null &&
                incomingMessage.metaData.details.replyDestination != null &&
                incomingMessage.metaData.details.correlationId != null;
    }

    private <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
        Consumer<ReplyType> createReplyHandlerFor(IncomingMessage<RequestType, IncomingMessageMetaData> request) {
        SendInfo sendingInfo = new SendInfo(
                request.metaData.details.correlationId,
                null,
                null,
                null,
                null,
                null);
        OutgoingMessageMetaData meta = new OutgoingMessageMetaData(request.metaData.details.replyDestination, sendingInfo);
        return reply -> messageSender.doSend(reply, meta).subscribe();
    }

    private Consumer<Throwable> createErrorReplyHandlerFor(IncomingMessage<InternalMessageType, IncomingMessageMetaData> request) {
        SendInfo sendingInfo = new SendInfo(
                request.metaData.details.correlationId,
                null,
                null,
                null,
                null,
                null);
        OutgoingMessageMetaData meta = new OutgoingMessageMetaData(request.metaData.details.replyDestination, sendingInfo);
        return error -> messageSender.doSend(errorReplyFactory.apply(error), meta).subscribe();
    }


}
