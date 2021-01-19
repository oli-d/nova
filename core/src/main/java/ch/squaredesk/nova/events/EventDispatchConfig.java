/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

import io.reactivex.rxjava3.core.BackpressureStrategy;

import java.util.Objects;

public record EventDispatchConfig (
        BackpressureStrategy defaultBackpressureStrategy,
        boolean warnOnUnhandledEvents,
        EventDispatchMode eventDispatchMode,
        int parallelism) {

}
