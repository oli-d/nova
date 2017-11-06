/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets.annotation;

import io.reactivex.BackpressureStrategy;

public class EndpointDescriptor {
    public final String destination;
    public final boolean captureMetrics;
    public final BackpressureStrategy backpressureStrategy;

    public EndpointDescriptor(String destination, boolean captureMetrics, BackpressureStrategy backpressureStrategy) {
        this.destination = destination;
        this.captureMetrics = captureMetrics;
        this.backpressureStrategy = backpressureStrategy;
    }
}
