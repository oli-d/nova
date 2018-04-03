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

import java.net.URL;

public class OutgoingMessageMetaData extends ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData<URL, SendInfo> {

    public OutgoingMessageMetaData(URL origin) {
        this(origin, null);
    }

    public OutgoingMessageMetaData(URL origin, SendInfo details) {
        super(origin, details);
    }

}
