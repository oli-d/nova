/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

class JmsMessageMetaDataCreator {

    IncomingMessageMetaData<Destination, RetrieveInfo> createIncomingMessageMetaData(Message message) {
        try {
            return new IncomingMessageMetaData<>(
                            message.getJMSDestination(),
                            JmsSpecificInfoExtractor.extractFrom(message)
            );
        } catch (JMSException e) {
            throw new RuntimeException("Unable to parse incoming message", e);
        }
    }

}
