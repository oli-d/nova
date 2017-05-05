/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;

import static java.util.Objects.requireNonNull;

public class MessageSendingInfo<DestinationType, TransportSpecificInfoType> {
    public final DestinationType destination;
    public final TransportSpecificInfoType transportSpecificInfo;

    protected MessageSendingInfo(Builder<DestinationType, TransportSpecificInfoType> builder) {
        this.destination = builder.destination;
        this.transportSpecificInfo = builder.transportSpecificInfo;
    }

    public static class Builder<DestinationType, TransportSpecificInfoType> {
        private DestinationType destination;
        private TransportSpecificInfoType transportSpecificInfo;

        public Builder<DestinationType, TransportSpecificInfoType> withDestination(DestinationType destination) {
            this.destination = destination;
            return this;
        }

        public Builder<DestinationType, TransportSpecificInfoType> withTransportSpecificInfo(TransportSpecificInfoType transportSpecificInfo) {
            this.transportSpecificInfo = transportSpecificInfo;
            return this;
        }

        public MessageSendingInfo<DestinationType, TransportSpecificInfoType> build() {
            validate();
            return new MessageSendingInfo<>(this);
        }

        private void validate() {
            requireNonNull(destination, "destination must not be null");
        }
    }

    @Override
    public String toString() {
        return "{" +
                "destination=" + destination +
                ", transportSpecificInfo=" + transportSpecificInfo +
                '}';
    }
}
