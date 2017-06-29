/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events.annotation;

import ch.squaredesk.nova.metrics.SettableGauge;
import io.reactivex.functions.Consumer;

public class TimeMeasuringEventHandlingMethodInvoker implements Consumer<Object[]> {
    private final SettableGauge gauge;
    private final EventHandlingMethodInvoker delegate;

    public TimeMeasuringEventHandlingMethodInvoker(SettableGauge gauge, EventHandlingMethodInvoker delegate) {
        this.gauge = gauge;
        this.delegate = delegate;
    }

    @Override
    public void accept (Object... data) {
        long start = System.nanoTime();

        delegate.accept(data);

        long differenceInNanos = System.nanoTime() - start;
        gauge.setValue(differenceInNanos);
    }
}
