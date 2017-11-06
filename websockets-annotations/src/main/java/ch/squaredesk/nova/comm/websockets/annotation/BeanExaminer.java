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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

class BeanExaminer {
    private static final Predicate<Annotation> interestingAnnotation = anno -> anno instanceof OnEvent;

    static EndpointDescriptor[] websocketEndpointsIn(Object bean) {
        requireNonNull(bean, "bean to examine must not be null");

        return stream(bean.getClass().getDeclaredMethods())
                .filter(method -> stream(method.getDeclaredAnnotations()).anyMatch(interestingAnnotation))
//                .peek(method -> {
//                    if (!Modifier.isPublic(method.getModifiers()))
//                        throw new IllegalArgumentException(
//                                "Method " + prettyPrint(bean, method) + ", annotated with @" +
//                                        OnEvent.class.getSimpleName() + " must be public");
//                })
                .map(method -> {
                    OnEvent annotation = stream(method.getDeclaredAnnotations())
                            .filter(interestingAnnotation)
                            .findFirst()
                            .map(anno -> (OnEvent)anno)
                            .get();
                    return new EndpointDescriptor(
                            annotation.value(),
                            annotation.captureMetrics(),
                            annotation.backpressureStrategy());
                })
                .toArray(EndpointDescriptor[]::new);
    }

    private static String prettyPrint (Object bean, Method method) {
        StringBuilder sb = new StringBuilder(bean.getClass().getName())
            .append('.')
            .append(method.getName())
            .append('(')
            .append(Arrays.stream(method.getParameterTypes())
                    .map(paramterClass -> paramterClass.getSimpleName())
                    .collect(Collectors.joining(", ")))
            .append(')');
        return sb.toString();
    }
}
