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

import ch.squaredesk.nova.comm.DefaultMarshallerFactory;
import ch.squaredesk.nova.comm.ReflectionHelper;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.websockets.WebSocket;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

class BeanExaminer {
    private static final Predicate<Annotation> interestingAnnotation = anno -> anno instanceof OnMessage;

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

                    MessageMarshaller marshaller = instantiateMarshaller(method, annotation);
                    MessageUnmarshaller unmarshaller = instantiateUnmarshaller(method, annotation);

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

    private static MessageMarshaller instantiateMarshaller(Method method, OnMessage annotation) {
        if (!annotation.messageMarshallerClassName().isEmpty()) {
            return instantiateMarshaller(method, annotation.messageMarshallerClassName());
        } else {
//            Class<?> webSocketParamType = ReflectionHelper.getConcreteTypeOfGenericClassExtension(
//                    method.getParameterTypes()[1],
//                    0);
            return DefaultMarshallerFactory.getMarshallerForMessageType(method.getParameterTypes()[0]);
        }
    }

    private static MessageUnmarshaller instantiateUnmarshaller(Method method, OnMessage annotation)  {
        if (!annotation.messageUnmarshallerClassName().isEmpty()) {
            return instantiateUnmarshaller(method, annotation.messageUnmarshallerClassName());
        } else {
            return DefaultMarshallerFactory.getUnmarshallerForMessageType(method.getParameterTypes()[0]);
        }
    }

    private static boolean marshallerAcceptsType(MessageMarshaller<?,String> marshaller, Class paramTypeToCheckFor) {
        Class<?> concreteClass = ReflectionHelper.getConcreteTypeOfGenericInterfaceImplementation(
                marshaller,
                MessageMarshaller.class,
                0);

        return concreteClass != null && concreteClass.isAssignableFrom(paramTypeToCheckFor);
    }

    private static boolean unmarshallerReturnsType(MessageUnmarshaller unmarshaller, Class returnTypeToCheckFor) {
        Class<?> concreteClass = ReflectionHelper.getConcreteTypeOfGenericInterfaceImplementation(
                unmarshaller,
                MessageUnmarshaller.class,
                1);

        return concreteClass != null && concreteClass.isAssignableFrom(returnTypeToCheckFor);
    }

    private static MessageMarshaller instantiateMarshaller(Method method, String className) {
        Object instance = ReflectionHelper.instanceFromClassName(className);
        if (!(instance instanceof MessageMarshaller)) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageMarshaller");
        }
        MessageMarshaller<?, String> messageMarshaller = (MessageMarshaller)instance;
        if (!marshallerAcceptsType(messageMarshaller, method.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageMarshaller for method "
                    + method.getName());
        }
        return messageMarshaller;
    }

    private static MessageUnmarshaller instantiateUnmarshaller(Method method, String className)  {
        Object instance = ReflectionHelper.instanceFromClassName(className);
        if (!(instance instanceof MessageUnmarshaller)) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageUnmarshaller");
        }
        MessageUnmarshaller<String, ?> messageUnmarshaller = (MessageUnmarshaller) instance;
        if (!unmarshallerReturnsType(messageUnmarshaller, method.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageUnmarshaller for method "
                    + method.getName());
        }
        return messageUnmarshaller;
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
