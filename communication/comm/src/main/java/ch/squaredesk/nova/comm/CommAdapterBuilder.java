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

package ch.squaredesk.nova.comm;

import ch.squaredesk.nova.metrics.Metrics;

public abstract class CommAdapterBuilder<TransportMessageType, CommAdapterType extends CommAdapter<TransportMessageType>> {
    protected MessageTranscriber<TransportMessageType> messageTranscriber;
    protected Metrics metrics;

    protected CommAdapterBuilder() {
    }

    public CommAdapterBuilder<TransportMessageType, CommAdapterType> setMessageTranscriber(MessageTranscriber<TransportMessageType> transcriber) {
        this.messageTranscriber = transcriber;
        return this;
    }

    public CommAdapterBuilder<TransportMessageType, CommAdapterType> setMetrics(Metrics metrics) {
        this.metrics = metrics;
        return this;
    }

    private void baseValidate() {
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

    public MessageTranscriber<TransportMessageType> getMessageTranscriber() {
        return messageTranscriber;
    }

    public Metrics getMetrics() {
        return metrics;
    }
}
