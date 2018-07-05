/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.retrieving;

import static java.util.Objects.requireNonNull;

public class IncomingMessageMetaData<DestinationType, TransportSpecificDetailsType> {
    public final DestinationType destination;
    public final TransportSpecificDetailsType details;

    public IncomingMessageMetaData(DestinationType destination, TransportSpecificDetailsType details) {
        requireNonNull(destination, "destination must not be null");
        this.destination = destination;
        this.details = details;
    }

    @Override
    public String toString() {
        return "{destination=" + destination + ", details=" + details + '}';
    }
}
