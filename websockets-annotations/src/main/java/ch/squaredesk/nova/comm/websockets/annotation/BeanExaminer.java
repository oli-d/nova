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

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.ReflectionHelper;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import io.reactivex.functions.Function;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

class BeanExaminer {
    private static final Predicate<Annotation> interestingAnnotation = anno -> anno instanceof OnMessage;

    private final MessageTranscriber<String> defaultMessageTranscriber;

    BeanExaminer(MessageTranscriber<String> messageTranscriber) {
        this.defaultMessageTranscriber = messageTranscriber;
    }

    EndpointDescriptor[] websocketEndpointsIn(Object bean) {
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
                            .map(anno -> (OnMessage) anno)
                            .get();

                    Class<?> messageType = getMessageTypeFromHandlerMethod(method);
                    Function<?, String> marshaller = instantiateMarshaller(method, annotation, messageType);
                    Function<String, ?> unmarshaller = instantiateUnmarshaller(method, annotation, messageType);

                    return new EndpointDescriptor(
                            bean,
                            method,
                            annotation.value(),
                            messageType,
                            marshaller,
                            unmarshaller,
                            annotation.captureTimings(),
                            annotation.backpressureStrategy());
                })
                .toArray(EndpointDescriptor[]::new);
    }

    private static boolean methodSignatureValidForMessageHandler(Method m) {
        return m.getReturnType() == void.class &&
                m.getParameterTypes().length == 2 &&
                m.getParameterTypes()[1].isAssignableFrom(WebSocket.class);
    }

    private static Class<?> getMessageTypeFromHandlerMethod (Method method) {
        return method.getParameterTypes()[0];
    }

    private Function<?, String> instantiateMarshaller(Method method, OnMessage annotation, Class<?> messageType) {
        if (!annotation.messageMarshallerClassName().isEmpty()) {
            return instantiateMarshaller(method, annotation.messageMarshallerClassName());
        } else {
            return defaultMessageTranscriber.getOutgoingMessageTranscriber(messageType);
        }
    }

    private Function<String, ?> instantiateUnmarshaller(Method method, OnMessage annotation, Class<?> messageType) {
        if (!annotation.messageUnmarshallerClassName().isEmpty()) {
            return instantiateUnmarshaller(method, annotation.messageUnmarshallerClassName());
        } else {
            return defaultMessageTranscriber.getIncomingMessageTranscriber(messageType);
        }
    }

    private static boolean marshallerAcceptsType(Function<?, String> marshaller, Class paramTypeToCheckFor) {
        Class<?> concreteClass = ReflectionHelper.getConcreteTypeOfGenericInterfaceImplementation(
                marshaller,
                Function.class,
                0);

        return concreteClass != null && concreteClass.isAssignableFrom(paramTypeToCheckFor);
    }

    private static boolean unmarshallerReturnsType(Function<String, ?> unmarshaller, Class returnTypeToCheckFor) {
        Class<?> concreteClass = ReflectionHelper.getConcreteTypeOfGenericInterfaceImplementation(
                unmarshaller,
                Function.class,
                1);

        return concreteClass != null && concreteClass.isAssignableFrom(returnTypeToCheckFor);
    }

    private static Function<?, String> instantiateMarshaller(Method method, String className) {
        Object instance = ReflectionHelper.instanceFromClassName(className);
        if (!(instance instanceof Function)) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageMarshaller");
        }
        Function<?, String> messageMarshaller = (Function<?, String>) instance;
        if (!marshallerAcceptsType(messageMarshaller, method.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageMarshaller for method "
                    + method.getName());
        }
        return messageMarshaller;
    }

    private static Function<String, ?> instantiateUnmarshaller(Method method, String className) {
        Object instance = ReflectionHelper.instanceFromClassName(className);
        if (!(instance instanceof Function)) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageUnmarshaller");
        }
        Function<String, ?> messageUnmarshaller = (Function<String, ?>) instance;
        if (!unmarshallerReturnsType(messageUnmarshaller, method.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageUnmarshaller for method "
                    + method.getName());
        }
        return messageUnmarshaller;
    }

    private static String prettyPrint(Object bean, Method method) {
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
