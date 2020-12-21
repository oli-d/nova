/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;

import static java.util.Objects.requireNonNull;

public class OutgoingMessageMetaData<DestinationType, TransportSpecificInfoType> {
    public final DestinationType destination;
    public final TransportSpecificInfoType details;

    public OutgoingMessageMetaData(DestinationType destination, TransportSpecificInfoType details) {
        requireNonNull(destination, "destination must not be null");
        this.destination = destination;
        this.details = details;
    }

    @Override
    public String toString() {
        return "{destination=" + destination + ", details=" + details + '}';
    }
}
