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

import ch.squaredesk.nova.comm.bla.Invocation;

public class HttpInvocation<RequestType> extends Invocation<RequestType, String> {

    public HttpInvocation(RequestType request) {
        super(request);
    }

    @Override
    public <ReplyType> void complete(ReplyType reply, String meta) {
        return;
    }
}
