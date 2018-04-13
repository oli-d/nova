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

public interface RpcCompletor<MessageType> extends ch.squaredesk.nova.comm.rpc.RpcCompletor<MessageType, ReplyInfo> {

    default void complete(int statusCode, MessageType reply) {
        ReplyInfo replyInfo = new ReplyInfo(statusCode);
        complete(reply, replyInfo);
    }
}
