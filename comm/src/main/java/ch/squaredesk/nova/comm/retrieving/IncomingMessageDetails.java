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

public class IncomingMessageDetails<DestinationType, TransportSpecificDetailsType> {
    public final DestinationType destination;
    public final TransportSpecificDetailsType transportSpecificDetails;

    @Override
    public String toString() {
        return "IncomingMessageDetails [destination=" + destination +
                ", transportSpecificDetails=" + transportSpecificDetails + "]";
    }

    private IncomingMessageDetails(Builder<DestinationType, TransportSpecificDetailsType> builder) {
        this.destination = builder.destination;
        this.transportSpecificDetails = builder.transportSpecificDetails;
    }

    public static class Builder<DestinationType, TransportSpecificDetailsType> {

        private DestinationType destination;
        private TransportSpecificDetailsType transportSpecificDetails;

        public Builder<DestinationType, TransportSpecificDetailsType> withDestination(DestinationType destination) {
            this.destination = destination;
            return this;
        }

        public Builder<DestinationType, TransportSpecificDetailsType> withTransportSpecificDetails(TransportSpecificDetailsType transportSpecificDetailsType) {
            this.transportSpecificDetails = transportSpecificDetailsType;
            return this;
        }

        public IncomingMessageDetails<DestinationType, TransportSpecificDetailsType> build() {
            return new IncomingMessageDetails<>(this);
        }
    }

}
