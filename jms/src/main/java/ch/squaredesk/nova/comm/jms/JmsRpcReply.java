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

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.rpc.RpcReply;

import javax.jms.Destination;

public class JmsRpcReply<T> extends RpcReply<T, Destination, JmsSpecificInfo> {
    public JmsRpcReply(T result, IncomingMessageMetaData<Destination, JmsSpecificInfo> metaData) {
        super(result, metaData);
    }
}
