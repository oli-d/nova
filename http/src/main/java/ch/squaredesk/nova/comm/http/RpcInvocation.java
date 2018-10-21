/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.tuples.Pair;

import java.util.Map;
import java.util.function.Consumer;

public class RpcInvocation<IncomingMessageType>
        extends ch.squaredesk.nova.comm.rpc.RpcInvocation<IncomingMessageType, RequestMessageMetaData, String, ReplyInfo>
        implements RpcCompletor {


    public RpcInvocation(IncomingMessage<IncomingMessageType, RequestMessageMetaData> request, Consumer<Pair<String, ReplyInfo>> replyConsumer, Consumer<Throwable> errorConsumer) {
        super(request, replyConsumer, errorConsumer);
    }

    <T> void complete(T reply, MessageMarshaller<T, String> marshaller, int statusCode) throws Exception {
        complete(reply, marshaller, statusCode, null);
    }


    <T> void complete(T reply, MessageMarshaller<T, String> marshaller, Map<String, String> replyHeaders) throws Exception {
        complete(reply, marshaller, 200, replyHeaders);
    }

    <T> void complete(T reply, MessageMarshaller<T, String> marshaller, int statusCode, Map<String, String> replyHeaders) throws Exception {
        ReplyInfo replyInfo = new ReplyInfo(statusCode, replyHeaders);
        complete(reply, marshaller, replyInfo);
    }

    <T> void complete(T reply, MessageMarshaller<T, String> marshaller, ReplyInfo replySpecificInfo) throws Exception;

    //    protected RpcInvocation(IncomingMessage<InternalMessageType,RequestMessageMetaData> request,
//                            Consumer<Pair<InternalMessageType, ReplyInfo>> replyConsumer,
//                            Consumer<Throwable> errorConsumer) {
//        super(request, replyConsumer, errorConsumer);
//    }
//
//    public void complete(int statusCode, InternalMessageType reply) {
//        ReplyInfo replyInfo = new ReplyInfo(statusCode);
//        complete(reply, replyInfo);
//    }
}
