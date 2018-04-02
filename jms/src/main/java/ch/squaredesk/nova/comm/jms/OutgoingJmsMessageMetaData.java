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

import ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData;

import javax.jms.Destination;

public class OutgoingJmsMessageMetaData extends OutgoingMessageMetaData<Destination, JmsSpecificInfo> {
    protected OutgoingJmsMessageMetaData(Builder builder) {
        super(builder);
    }

    public static class Builder extends OutgoingMessageMetaData.Builder<Destination, JmsSpecificInfo> {
    }

}
