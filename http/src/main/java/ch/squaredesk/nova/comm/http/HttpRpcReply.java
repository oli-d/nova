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

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.rpc.RpcReply;

import java.net.URL;

public class HttpRpcReply<T> extends RpcReply<T, URL, HttpSpecificRetrievalInfo> {
    public HttpRpcReply(T result, IncomingMessageMetaData<URL, HttpSpecificRetrievalInfo> metaData) {
        super(result, metaData);
    }
}
