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

public class IncomingMessage<InternalMessageType, DestinationType, TransportSpecificDetailsType> {
    public final InternalMessageType message;
    public final IncomingMessageDetails<DestinationType, TransportSpecificDetailsType> details;


    public IncomingMessage(InternalMessageType message, IncomingMessageDetails<DestinationType, TransportSpecificDetailsType> details) {
        this.message = message;
        this.details = details;
    }

    @Override
    public String toString() {
        return "IncomingMessage{" +
                "message=" + message +
                ", details=" + details +
                '}';
    }
}
