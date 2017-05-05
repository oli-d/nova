/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics;

import com.codahale.metrics.Gauge;

import java.util.concurrent.atomic.AtomicLong;

public class SettableGauge implements Gauge<Long> {
    private final AtomicLong value = new AtomicLong();

    @Override
    public Long getValue() {
        return value.get();
    }

    public void setValue(long value) {
        this.value.set(value);
    }
}
