/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

class BeanExaminer {
    Method[] shutdownHandlersIn (Object bean) {
        return handlersIn(bean, OnServiceShutdown.class);
    }

    Method[] startupHandlersIn (Object bean) {
        return handlersIn(bean, OnServiceStartup.class);
    }

    Method[] initHandlersIn (Object bean) {
        return handlersIn(bean, OnServiceInit.class);
    }

    private Method[] handlersIn (Object bean, Class expectedAnnotationClass) {
        requireNonNull(bean, "bean to examine must not be null");

        return stream(bean.getClass().getDeclaredMethods())
                .filter(method -> stream(method.getDeclaredAnnotations()).anyMatch(
                        anno -> expectedAnnotationClass.isAssignableFrom(anno.getClass())))
                .peek(method -> {
                    if (!Modifier.isPublic(method.getModifiers()))
                        throw new IllegalArgumentException(
                                "Method " + prettyPrint(bean, method) + ", annotated with @" +
                                expectedAnnotationClass.getSimpleName() + " must be public");
                })
                .peek(method -> {
                    if (method.getParameterCount()>0)
                        throw new IllegalArgumentException(
                                "Method " + prettyPrint(bean, method) + ", annotated with @" +
                                expectedAnnotationClass.getSimpleName() + " must not declare any parameters");
                })
                .toArray(Method[]::new);
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
