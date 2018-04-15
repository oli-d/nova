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

    private static boolean isPublic (Method m) {
        return Modifier.isPublic(m.getModifiers());
    }

    private static boolean returnsVoid (Method m) {
        return m.getReturnType().equals(void.class) || m.getReturnType().equals(Void.class);
    }

    private static boolean acceptsTwoParameters(Method m) {
        return m.getParameterCount() == 2;
    }

    private static boolean isRpcCompletor(Class<?> classToTest) {
        return RpcCompletor.class.isAssignableFrom(classToTest);
    }

    private static boolean parameterOneHasType(Method m, Class<?> typeToVerify) {
        Class<?> parameterType = m.getParameterTypes()[0];
        return parameterType.isAssignableFrom(typeToVerify);
    }

    private static boolean parameterTwoIsRpcCompletor(Method m) {
        Class<?> parameterType = m.getParameterTypes()[1];
        return isRpcCompletor(parameterType);
    }

    private static void ensureProperRpcHandlerFunction (Class<?> requestTypeClass, Object bean, Method m) {
        if (!isPublic(m)) {
            throw new IllegalArgumentException("Annotated RPC request handler method " + prettyPrint(bean, m) + " must be public");
        }
        if (!(returnsVoid(m) &&
                acceptsTwoParameters(m) &&
                parameterOneHasType(m, requestTypeClass) &&
                parameterTwoIsRpcCompletor(m))) {
            throw new IllegalArgumentException("Annotated RPC request handler method " + prettyPrint(bean, m)
                    + " must be a BiConsumer<" + requestTypeClass.getSimpleName() + ", RpcCompletor>");
        }
    }

    private static String prettyPrint(Object bean, Method method) {
        return bean.getClass().getName() + '.' +
                method.getName() + '(' +
                Arrays.stream(method.getParameterTypes())
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", ")) +
                ')';
    }
}
