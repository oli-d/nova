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

import io.reactivex.BackpressureStrategy;

import java.lang.reflect.Method;

public interface EventHandlingConfigConsumer {
    void accept(String event,
                Object objectToInvokeMethodOn, Method m,
                BackpressureStrategy backpressureStrategy,
                boolean dispatchOnBusinessLogicThread,
                boolean measureTime);
}
