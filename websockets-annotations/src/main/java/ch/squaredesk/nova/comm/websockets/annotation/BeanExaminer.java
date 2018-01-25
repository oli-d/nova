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

package ch.squaredesk.nova.comm.websockets.annotation;

import ch.squaredesk.nova.comm.websockets.WebSocket;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

class BeanExaminer {
    private static final Predicate<Annotation> interestingAnnotation = anno -> anno instanceof OnMessage;

    private BeanExaminer() {
    }

    static EndpointDescriptor[] websocketEndpointsIn(Object bean) {
        requireNonNull(bean, "bean to examine must not be null");

        return stream(bean.getClass().getDeclaredMethods())
                .filter(method -> stream(method.getDeclaredAnnotations()).anyMatch(interestingAnnotation))
                .peek(method -> {
                    if (!methodSignatureValidForMessageHandler(method))
                        throw new IllegalArgumentException(
                                "Method " + prettyPrint(bean, method) + ", annotated with @" +
                                        OnMessage.class.getSimpleName() + " has an invalid signature");
                })
                .map(method -> {
                    OnMessage annotation = stream(method.getDeclaredAnnotations())
                            .filter(interestingAnnotation)
                            .findFirst()
                            .map(anno -> (OnMessage)anno)
                            .get();

                    return new EndpointDescriptor(
                            bean,
                            method,
                            annotation.value(),
                            annotation.captureTimings(),
                            annotation.backpressureStrategy());
                })
                .toArray(EndpointDescriptor[]::new);
    }

    private static boolean methodSignatureValidForMessageHandler (Method m) {
        return m.getReturnType() == void.class &&
                m.getParameterTypes().length == 2 &&
                m.getParameterTypes()[1].isAssignableFrom(WebSocket.class);
    }

    private static String prettyPrint (Object bean, Method method) {
        StringBuilder sb = new StringBuilder(bean.getClass().getName())
            .append('.')
            .append(method.getName())
            .append('(')
            .append(stream(method.getParameterTypes())
                    .map(paramterClass -> paramterClass.getSimpleName())
                    .collect(Collectors.joining(", ")))
            .append(')');
        return sb.toString();
    }
}
