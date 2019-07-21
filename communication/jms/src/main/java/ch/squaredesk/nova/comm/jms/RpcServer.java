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

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.metrics.Metrics;
import io.dropwizard.metrics5.Timer;
import io.reactivex.Flowable;

import javax.jms.Destination;

import static java.util.Objects.requireNonNull;

public class RpcServer extends ch.squaredesk.nova.comm.rpc.RpcServer<Destination, String> {

    private final MessageSender messageSender;
    private final MessageReceiver messageReceiver;

    RpcServer(String identifier,
              MessageReceiver messageReceiver,
              MessageSender messageSender,
              Metrics metrics) {
        this(identifier, messageReceiver, messageSender, new DefaultMessageTranscriberForStringAsTransportType(), metrics);
    }

    RpcServer(String identifier,
              MessageReceiver messageReceiver,
              MessageSender messageSender,
              MessageTranscriber<String> messageTranscriber,
              Metrics metrics) {
        super(Metrics.name("jms", identifier).toString(), messageTranscriber, metrics);

        requireNonNull(messageSender, "messageSender must not be null");
        requireNonNull(messageReceiver, "messageReceiver must not be null");
        this.messageSender = messageSender;
        this.messageReceiver = messageReceiver;
    }

    @Override
    public <T> Flowable<RpcInvocation<T>> requests(Destination destination, Class<T> requestType) {
        return messageReceiver.messages(destination, messageTranscriber.getIncomingMessageTranscriber(requestType))
                .filter(this::isRpcRequest)
                .map(incomingRequest -> {
                    Timer.Context timerContext = metricsCollector.requestReceived(incomingRequest.metaData.destination);
                    return new RpcInvocation<>(
                            incomingRequest,
                            reply -> {
                                SendInfo sendingInfo = new SendInfo(
                                        incomingRequest.metaData.details.correlationId,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null);
                                OutgoingMessageMetaData meta = new OutgoingMessageMetaData(incomingRequest.metaData.details.replyDestination, sendingInfo);
                                messageSender.send(reply._1, meta).subscribe();
                                metricsCollector.requestCompleted(timerContext, reply);
                            },
                            error -> {
                                // TODO: Is there a sensible default action we could perform?
                                metricsCollector.requestCompletedExceptionally(timerContext, incomingRequest.metaData.destination, error);
                            });
                });
    }

    private <T> boolean isRpcRequest(IncomingMessage<T, IncomingMessageMetaData> incomingMessage) {
        return incomingMessage.metaData != null &&
                incomingMessage.metaData.details != null &&
                incomingMessage.metaData.details.replyDestination != null &&
                incomingMessage.metaData.details.correlationId != null;
    }
}
