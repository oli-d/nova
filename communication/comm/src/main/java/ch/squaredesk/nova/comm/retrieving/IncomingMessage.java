/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.retrieving;

public class IncomingMessage<MessageType, MetaDataType extends IncomingMessageMetaData<?,?>> {
    public final MessageType message;
    public final MetaDataType metaData;


    public IncomingMessage(MessageType message, MetaDataType metaData) {
        this.message = message;
        this.metaData = metaData;
    }

    @Override
    public String toString() {
        return "IncomingMessage{" +
                "message=" + message +
                ", metaData=" + metaData +
                '}';
    }
}
