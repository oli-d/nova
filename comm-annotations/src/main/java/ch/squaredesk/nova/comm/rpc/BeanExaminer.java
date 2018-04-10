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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

    private static boolean onlyAcceptsSingleParameter(Method m) {
        return m.getParameterCount() == 1;
    }

    private static boolean isRpcIncvocation (Class<?> classToTest) {
        return RpcInvocation.class.isAssignableFrom(classToTest);
    }

    private Class getRpcInvocationParameterType (Type rpcInvocationType, int parameterIndex) {
        ParameterizedType genericSuperclass;
        if (rpcInvocationType instanceof ParameterizedType) {
            genericSuperclass = (ParameterizedType) rpcInvocationType;
        } else {
            genericSuperclass = (ParameterizedType) ((Class)rpcInvocationType).getGenericSuperclass();
        }

        return (Class)genericSuperclass.getActualTypeArguments()[parameterIndex];
    }

    private boolean parameterIsProperlyTypedRpcInvocation(Method m, Class<?> incomingRequestType) {
        Class<?> parameterType = m.getParameterTypes()[0];
        Type genericParameterType = m.getGenericParameterTypes()[0];

        return isRpcIncvocation(parameterType) &&
                getRpcInvocationParameterType(genericParameterType,0).isAssignableFrom(incomingRequestType);
    }

    private void ensureProperRpcHandlerFunction (Class<?> registeredRequestTypeClass, Object bean, Method m) {
        if (!isPublic(m)) {
            throw new IllegalArgumentException("Annotated RPC request handler method " + prettyPrint(bean, m) + " must be public");
        }
        if (!(returnsVoid(m) && onlyAcceptsSingleParameter(m) && parameterIsProperlyTypedRpcInvocation(m, registeredRequestTypeClass))) {
            throw new IllegalArgumentException("Annotated RPC request handler method " + prettyPrint(bean, m)
                    + " must be a Consumer<RpcInvocation<" + registeredRequestTypeClass.getName() + ",?,?,?>");
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
