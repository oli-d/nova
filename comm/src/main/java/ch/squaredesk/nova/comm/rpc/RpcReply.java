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

package ch.squaredesk.nova.comm.rpc;

public class RpcReply<ReplyType, MetaDataType> {
    public final ReplyType result;
    public final MetaDataType metaData;

    public RpcReply(ReplyType result, MetaDataType metaData) {
        this.result = result;
        this.metaData = metaData;
    }

    @Override
    public String toString() {
        return "{ result=" + result + ", metaData=" + metaData + '}';
    }
}
