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

package ch.squaredesk.nova.comm.bla;

public abstract class Invocation<RequestType, ReplyMetaType> {
    public final RequestType request;

    public Invocation(RequestType request) {
        this.request = request;
    }

    public abstract <ReplyType> void complete (ReplyType reply, ReplyMetaType meta);
}
