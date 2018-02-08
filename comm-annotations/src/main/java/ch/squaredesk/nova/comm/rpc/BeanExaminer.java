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

package ch.squaredesk.nova.comm.rpc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

class BeanExaminer {
    RpcRequestHandlerDescription[] examine(Object bean) {
        Objects.requireNonNull(bean, "bean to examine must not be null");

        Method[] methods = bean.getClass().getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(OnRpcInvocation.class))
                .map(m -> {
                    OnRpcInvocation annotation = m.getAnnotation(OnRpcInvocation.class);
                    ensureProperRpcHandlerFunction(annotation.value(), bean, m);
                    return new RpcRequestHandlerDescription(
                            annotation.value(),
                            bean,
                            m);
                })
                .toArray(RpcRequestHandlerDescription[]::new);
    }

    private void ensureProperRpcHandlerFunction (Class<?> registeredRequestTypeClass, Object bean, Method m) {
        if (!Modifier.isPublic(m.getModifiers())) {
            throw new IllegalArgumentException("Annotated RPC request handler method " + prettyPrint(bean, m) + " must be public");
        }
        if (m.getReturnType().equals(void.class) || m.getReturnType().equals(Void.class)) {
            throw new IllegalArgumentException("Annotated RPC request handler method " + prettyPrint(bean, m)
                    + " must be a function RequestType -> ReplyType");
        }
        if (m.getParameterCount() != 1) {
            throw new IllegalArgumentException("Annotated RPC request handler method " + prettyPrint(bean, m)
                    + " must be a function RequestType -> ReplyType");
        }
        Class<?> requestClass = m.getParameterTypes()[0];
        if (!registeredRequestTypeClass.isAssignableFrom(requestClass)) {
            throw new IllegalArgumentException("Parameter type of annotated RPC request handler method " + prettyPrint(bean, m)
                    + " must be " + registeredRequestTypeClass.getName() + " or a subclass");
        }
    }

    private static String prettyPrint(Object bean, Method method) {
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
