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

public class RequestMessageMetaData extends ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData<URL, RequestInfo> {

    public RequestMessageMetaData(URL destination) {
        this(destination, null);
    }

    public RequestMessageMetaData(URL destination, RequestInfo details) {
        super(destination, details);
    }

}
