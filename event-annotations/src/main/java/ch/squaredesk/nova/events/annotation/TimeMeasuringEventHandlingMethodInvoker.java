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

import com.codahale.metrics.Timer;
import io.reactivex.functions.Consumer;

public class TimeMeasuringEventHandlingMethodInvoker implements Consumer<Object[]> {
    private final Timer timer;
    private final EventHandlingMethodInvoker delegate;

    public TimeMeasuringEventHandlingMethodInvoker(Timer timer, EventHandlingMethodInvoker delegate) {
        this.timer = timer;
        this.delegate = delegate;
    }

    @Override
    public void accept (Object... data) {
        Timer.Context ctx = timer.time();

        delegate.accept(data);

        ctx.stop();
    }
}
