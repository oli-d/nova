/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.core.events;

import ch.squaredesk.nova.events.OnEvent;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

class BeanExaminer {
    EventHandlerDescription[] examine(Object bean) {
        Objects.requireNonNull(bean, "bean to examine must not be null");

        Method[] methods = bean.getClass().getMethods();
        return Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(OnEvent.class))
                .peek(m -> {
                    if (m.getAnnotation(OnEvent.class).value().length == 0) {
                        throw new IllegalArgumentException("Invalid ch.squaredesk.nova.events.ch.squaredesk.nova.events.annotation definition: no event key provided");
                    }
                })
                .map(m -> {
                    OnEvent annotation = m.getAnnotation(OnEvent.class);
                    return new EventHandlerDescription(
                            bean,
                            m,
                            annotation.value(),
                            annotation.backpressureStrategy(),
                            annotation.enableInvocationTimeMetrics());
                })
                .toArray(EventHandlerDescription[]::new);
    }
}
