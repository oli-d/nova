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

public abstract class CommAdapter<TransportMessageType> {
    protected MarshallerRegistry<TransportMessageType> marshallerRegistry;
    protected Metrics metrics;

    protected CommAdapter(MarshallerRegistry<TransportMessageType> marshallerRegistry, Metrics metrics) {
        this.marshallerRegistry = marshallerRegistry;
        this.metrics = metrics;
    }
}
