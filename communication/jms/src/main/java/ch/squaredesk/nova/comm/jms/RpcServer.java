/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData;
import ch.squaredesk.nova.metrics.MetricsName;
import io.micrometer.core.instrument.Timer;
import io.reactivex.rxjava3.core.Flowable;

import javax.jms.Destination;

import static java.util.Objects.requireNonNull;

public class RpcServer extends ch.squaredesk.nova.comm.rpc.RpcServer<Destination, String> {

    private final MessageSender messageSender;
    private final MessageReceiver messageReceiver;

    RpcServer(String identifier,
              MessageReceiver messageReceiver,
              MessageSender messageSender) {
        this(identifier, messageReceiver, messageSender, new DefaultMessageTranscriberForStringAsTransportType());
    }

    RpcServer(String identifier,
              MessageReceiver messageReceiver,
              MessageSender messageSender,
              MessageTranscriber<String> messageTranscriber) {
        super(MetricsName.buildName("jms", identifier), messageTranscriber);

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
                    metricsCollector.requestReceived(incomingRequest.metaData().destination());
                    Timer.Sample timerContext = Timer.start();
                    return new RpcInvocation<>(
                            incomingRequest,
                            reply -> {
                                SendInfo sendingInfo = new SendInfo(
                                        incomingRequest.metaData().details().correlationId(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null);
                                OutgoingMessageMetaData<Destination, SendInfo> meta = new OutgoingMessageMetaData(
                                        incomingRequest.metaData().details().replyDestination(),
                                        sendingInfo);
                                messageSender.send(reply.item1(), meta).subscribe();
                                metricsCollector.requestCompleted(timerContext, destination, reply);
                            },
                                // TODO: Is there a sensible default action we could perform?
                            error -> metricsCollector.requestCompletedExceptionally(timerContext, destination, error));
                });
    }

    private <T> boolean isRpcRequest(IncomingMessage<T, IncomingMessageMetaData<Destination, RetrieveInfo>> incomingMessage) {
        return incomingMessage.metaData() != null &&
                incomingMessage.metaData().details() != null &&
                incomingMessage.metaData().details().replyDestination() != null &&
                incomingMessage.metaData().details().correlationId() != null;
    }
}
