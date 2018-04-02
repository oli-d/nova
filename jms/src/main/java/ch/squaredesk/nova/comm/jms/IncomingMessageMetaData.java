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

import javax.jms.Destination;

public class IncomingMessageMetaData extends ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData<Destination, JmsSpecificInfo> {
    public IncomingMessageMetaData(Destination destination) {
        this(destination, null);
    }

    public IncomingMessageMetaData(Destination destination, JmsSpecificInfo sendDetails) {
        super(destination, sendDetails);
    }
}
