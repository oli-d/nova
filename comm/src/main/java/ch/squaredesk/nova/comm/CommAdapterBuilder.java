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

import ch.squaredesk.nova.metrics.Metrics;

public abstract class CommAdapterBuilder<MessageType, CommAdapterType> {
    private final Class<MessageType> messageTypeClass;

    public DefaultMarshallerRegistryForStringAsTransportType marshallerRegistry;
    public Metrics metrics;

    protected CommAdapterBuilder(Class<MessageType> messageTypeClass) {
        this.messageTypeClass = messageTypeClass;
    }

    public CommAdapterBuilder<MessageType, CommAdapterType> setMarshallerRegistry(DefaultMarshallerRegistryForStringAsTransportType registry) {
        this.marshallerRegistry = registry;
        return this;
    }

    public CommAdapterBuilder<MessageType, CommAdapterType> setMetrics(Metrics metrics) {
        this.metrics = metrics;
        return this;
    }

    private void baseValidate() {
        if (marshallerRegistry == null) {
            marshallerRegistry = new DefaultMarshallerRegistryForStringAsTransportType();
        }
        if (metrics == null) {
            metrics = new Metrics();
        }
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
