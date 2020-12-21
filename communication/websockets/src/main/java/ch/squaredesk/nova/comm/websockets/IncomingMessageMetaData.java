/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.websockets;

public class IncomingMessageMetaData
        extends ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData<String, RetrieveInfo> {

    public IncomingMessageMetaData(String origin) {
        this(origin, null);
    }

    public IncomingMessageMetaData(String origin, RetrieveInfo sendDetails) {
        super(origin, sendDetails);
    }
}
