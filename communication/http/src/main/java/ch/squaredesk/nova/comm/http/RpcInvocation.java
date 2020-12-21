/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.tuples.Pair;

import java.util.Map;
import java.util.function.Consumer;

public class RpcInvocation<IncomingMessageType>
        extends ch.squaredesk.nova.comm.rpc.RpcInvocation<IncomingMessageType, RequestMessageMetaData, String, ReplyInfo> {

    private final MessageTranscriber<String> transcriber;

    public RpcInvocation(IncomingMessage<IncomingMessageType, RequestMessageMetaData> request,
                         // TODO: make this a BiConsumer instead of a Consumer<Pair>
                         Consumer<Pair<String, ReplyInfo>> replyConsumer,
                         Consumer<Throwable> errorConsumer,
                         MessageTranscriber<String> transcriber) {
        super(request, replyConsumer, errorConsumer);
        this.transcriber = transcriber;
    }

    public <T> void complete(T reply) throws Throwable {
        complete(reply, 200, null);
    }

    public <T> void complete(T reply, int statusCode) throws Throwable {
        complete(reply, statusCode, null);
    }


    public <T> void complete(T reply, Map<String, String> replyHeaders) throws Throwable {
        complete(reply, 200, replyHeaders);
    }

    public <T> void complete(T reply, int statusCode, Map<String, String> replyHeaders) throws Throwable {
        ReplyInfo replyInfo = new ReplyInfo(statusCode, replyHeaders);
        complete(reply, replyInfo, transcriber.getOutgoingMessageTranscriber(reply));
    }

    public void complete(String reply) {
        complete(reply, 200, null);
    }

    public void complete(String reply, int statusCode) {
        complete(reply, statusCode, null);
    }


    public void complete(String reply, Map<String, String> replyHeaders) {
        complete(reply, 200, replyHeaders);
    }

    public void complete(String reply, int statusCode, Map<String, String> replyHeaders) {
        ReplyInfo replyInfo = new ReplyInfo(statusCode, replyHeaders);
        complete(reply, replyInfo);
    }
}
