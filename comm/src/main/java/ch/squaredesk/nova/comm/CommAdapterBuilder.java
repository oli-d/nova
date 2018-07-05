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

package ch.squaredesk.nova.comm;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.metrics.Metrics;

import static java.util.Objects.requireNonNull;

public abstract class CommAdapterBuilder<MessageType, CommAdapterType> {
    private final Class<MessageType> messageTypeClass;

    public MessageMarshaller<MessageType, String> messageMarshaller;
    public MessageUnmarshaller<String, MessageType> messageUnmarshaller;
    public Metrics metrics;

    protected CommAdapterBuilder(Class<MessageType> messageTypeClass) {
        this.messageTypeClass = messageTypeClass;
    }

    public CommAdapterBuilder<MessageType, CommAdapterType> setMessageMarshaller(MessageMarshaller<MessageType, String> marshaller) {
        this.messageMarshaller = marshaller;
        return this;
    }

    public CommAdapterBuilder<MessageType, CommAdapterType> setMessageUnmarshaller(MessageUnmarshaller<String, MessageType> unmarshaller) {
        this.messageUnmarshaller = unmarshaller;
        return this;
    }

    public CommAdapterBuilder<MessageType, CommAdapterType> setMetrics(Metrics metrics) {
        this.metrics = metrics;
        return this;
    }

    private void baseValidate() {
        if (messageMarshaller == null) {
            messageMarshaller = (MessageMarshaller<MessageType, String>)
                    DefaultMarshallerFactory.getMarshallerForMessageType(messageTypeClass);
        }
        if (messageUnmarshaller == null) {
            messageUnmarshaller = (MessageUnmarshaller<String, MessageType>)
                    DefaultMarshallerFactory.getUnmarshallerForMessageType(messageTypeClass);
        }
        if (metrics == null) {
            metrics = new Metrics();
        }
        requireNonNull(messageMarshaller, " messageMarshaller instance must not be null");
        requireNonNull(messageUnmarshaller, " messageUnmarshaller instance must not be null");
    }

    /**
     * Extension point for sub classes
     */
    protected void validate() {
    }

    protected abstract CommAdapterType createInstance();

    public final CommAdapterType build() {
        baseValidate();
        validate();
        return createInstance();
    }
}
