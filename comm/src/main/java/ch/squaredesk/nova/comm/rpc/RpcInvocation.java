/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import java.util.function.Consumer;

public class RpcInvocation<RequestType, ReplyType, TransportSpecificInfoType> {
    public final RequestType request;
    public final TransportSpecificInfoType transportSpecificInfo;
    private final Consumer<ReplyType> replyConsumer;
    private final Consumer<Throwable> errorConsumer;

    public RpcInvocation(RequestType request, TransportSpecificInfoType transportSpecificInfo,
                         Consumer<ReplyType> replyConsumer, Consumer<Throwable> errorConsumer) {
        this.request = request;
        this.transportSpecificInfo = transportSpecificInfo;
        this.replyConsumer = replyConsumer;
        this.errorConsumer = errorConsumer;
    }

    public void complete(ReplyType reply) {
        replyConsumer.accept(reply);
    }

    public void completeExceptionally(Throwable error) {
        errorConsumer.accept(error);
    }
}
