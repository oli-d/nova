/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.eventannotations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

class BeanExaminer {
    void examine (Object bean, EventHandlingConfigConsumer configConsumer) {
        Objects.requireNonNull(bean, "bean to examine must not be null");
        Objects.requireNonNull(configConsumer, "configConsumer must not be null");

        Method[] methods = bean.getClass().getMethods();
        Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(OnEvent.class))
                .forEach(m -> {
                    OnEvent annotation = m.getAnnotation(OnEvent.class);

                    if (annotation.value().length==0) {
                        throw new IllegalArgumentException("Invalid annotation definition. No " +
                                "event key provided");
                    } else {
                        Arrays.stream(annotation.value()).forEach(
                                event -> configConsumer.accept(event,
                                        bean, m,
                                        annotation.backpressureStrategy(),
                                        annotation.dispatchOnBusinessLogicThread(),
                                        annotation.enableInvocationTimeMetrics())
                        );
                    }
                });
    }
}
