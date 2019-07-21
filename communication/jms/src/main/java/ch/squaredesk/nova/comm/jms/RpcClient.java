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
import io.reactivex.functions.Function;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

public class RpcClient extends ch.squaredesk.nova.comm.rpc.RpcClient<String, OutgoingMessageMetaData, IncomingMessageMetaData> {
    private final MessageSender messageSender;
    private final MessageReceiver messageReceiver;

    RpcClient(String identifier,
                     MessageSender messageSender,
                     MessageReceiver messageReceiver,
                     Metrics metrics) {
        super(Metrics.name("jms", identifier).toString(), metrics);
        this.messageSender = messageSender;
        this.messageReceiver = messageReceiver;
    }

    @Override
    public <RequestType, ReplyType> Single<RpcReply<ReplyType>> sendRequest(
            RequestType request,
            OutgoingMessageMetaData requestMetaData,
            Function<RequestType, String> requestTranscriber,
            Function<String, ReplyType> replyTranscriber,
            long timeout, TimeUnit timeUnit) {

        requireNonNull(timeUnit, "timeUnit must not be null");
        requireNonNull(requestMetaData, "metaData must not be null");
        requireNonNull(requestMetaData.details, "metaData.details must not be null");
        requireNonNull(requestMetaData.details.correlationId, "correlationId must not be null");
        requireNonNull(requestMetaData.details.replyDestination, "replyDestination must not be null");

        // listen to RPC reply. This must be done BEFORE sending the request, otherwise we could miss a very fast response
        // if the Observable is hot
        String metricsInfo = String.valueOf(requestMetaData.destination) + "." + String.valueOf(request);
        Single<RpcReply<ReplyType>> replySingle =
                messageReceiver.messages(requestMetaData.details.replyDestination, replyTranscriber)
                .filter(incomingMessage ->
                        incomingMessage.metaData.details != null &&
                                requestMetaData.details.correlationId.equals(incomingMessage.metaData.details.correlationId))
                .take(1)
                .doOnNext(reply -> metricsCollector.rpcCompleted(metricsInfo, reply))
                .map(incomingMessage -> new RpcReply<>(incomingMessage.message, incomingMessage.metaData))
                .singleOrError();

        // send message sync
        try {
            messageSender
                    .send(request, requestMetaData, requestTranscriber)
                    .blockingGet();
        } catch (Exception e) {
            return Single.error(e);
        }

        return replySingle
                .timeout(timeout, timeUnit)
                .doOnError(t -> {
                    if (t instanceof TimeoutException) {
                        metricsCollector.rpcTimedOut(metricsInfo);
                    }
                });
    }

}
