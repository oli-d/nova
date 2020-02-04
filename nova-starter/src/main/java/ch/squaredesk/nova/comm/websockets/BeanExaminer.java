/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.ReflectionHelper;
import ch.squaredesk.nova.comm.websockets.annotation.OnClose;
import ch.squaredesk.nova.comm.websockets.annotation.OnConnect;
import ch.squaredesk.nova.comm.websockets.annotation.OnError;
import ch.squaredesk.nova.comm.websockets.annotation.OnMessage;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

class BeanExaminer {
    private final MessageTranscriber<String> defaultMessageTranscriber;

    BeanExaminer(MessageTranscriber<String> messageTranscriber) {
        this.defaultMessageTranscriber = messageTranscriber;
    }

    private static Consumer<Method> methodSignatureValidator (Predicate<Method> test,
                                                              java.util.function.Function<Method, String> errorMessageCreator) {
        return method -> {
            if (!test.test(method)) {
                throw new IllegalArgumentException(errorMessageCreator.apply(method));
            }
        };
    }

    Collection<OnMessageHandlerEndpointDescriptor> onMessageEndpointsIn(Object bean) {
        requireNonNull(bean, "bean to examine must not be null");

        return methodsWithAnnotation(bean, OnMessage.class)
                .doOnNext(methodSignatureValidator(
                    BeanExaminer::methodSignatureValidForMessageHandler,
                    method -> "Method " + prettyPrint(bean, method) + ", annotated with @" + OnMessage.class.getSimpleName() + " has an invalid signature"))
                .map(method -> {
                    OnMessage annotation = Observable.fromArray(method.getDeclaredAnnotations())
                            .filter(anno -> anno instanceof OnMessage)
                            .cast(OnMessage.class)
                            .firstElement()
                            .blockingGet();

                    Class<?> messageType = getMessageTypeFromHandlerMethod(method);
                    Function<?, String> marshaller = instantiateMarshaller(method, annotation, messageType);
                    Function<String, ?> unmarshaller = instantiateUnmarshaller(method, annotation, messageType);

                    return new OnMessageHandlerEndpointDescriptor(
                            bean,
                            method,
                            annotation.value(),
                            messageType,
                            marshaller,
                            unmarshaller,
                            annotation.captureTimings(),
                            annotation.logInvocations(),
                            annotation.backpressureStrategy());
                })
                .toList()
                .blockingGet();
    }

    Collection<EventHandlerEndpointDescriptor> onConnectHandlersIn(Object bean) {
        return eventHandlerEndpointsIn(
            bean,
            OnConnect.class,
            BeanExaminer::methodSignatureValidForConnectEventHandler
        );
    }

    Collection<EventHandlerEndpointDescriptor> onErrorHandlersIn(Object bean) {
        return eventHandlerEndpointsIn(
            bean,
            OnError.class,
            BeanExaminer::methodSignatureValidForErrorEventHandler
        );
    }

    Collection<EventHandlerEndpointDescriptor> onCloseHandlersIn(Object bean) {
        return eventHandlerEndpointsIn(
            bean,
            OnClose.class,
            BeanExaminer::methodSignatureValidForCloseEventHandler
        );
    }


    private <T extends Annotation> Collection<EventHandlerEndpointDescriptor> eventHandlerEndpointsIn(Object bean, Class<T> annotationType, Predicate<Method> methodSignatureValidator) {
        requireNonNull(bean, "bean to examine must not be null");

        return methodsWithAnnotation(bean, annotationType)
                .doOnNext(methodSignatureValidator(
                        methodSignatureValidator,
                        method -> "Method " + prettyPrint(bean, method) + ", annotated with @" + annotationType.getSimpleName() + " has an invalid signature")
                )
                .map(method -> {
                    T annotation = Observable.fromArray(method.getDeclaredAnnotations())
                            .filter(anno -> annotationType.isAssignableFrom(anno.getClass()))
                            .cast(annotationType)
                            .firstElement()
                            .blockingGet();

                    return new EventHandlerEndpointDescriptor(
                            bean,
                            method,
                            getDestinationFrom(annotation),
                            getCaptureTimings(annotation),
                            getLogInvocations(annotation));
                })
                .toList()
                .blockingGet();
    }

    private static Observable<Method> methodsWithAnnotation(Object bean, Class<?> annotationType) {
        requireNonNull(bean, "bean to examine must not be null");

        return Observable.fromArray(bean.getClass().getDeclaredMethods())
                .filter(method -> stream(method.getDeclaredAnnotations()).anyMatch(anno -> annotationType.isAssignableFrom(anno.getClass())));
    }

    private static boolean methodSignatureValidForMessageHandler(Method m) {
        return m.getReturnType() == void.class &&
                m.getParameterTypes().length == 2 &&
                m.getParameterTypes()[1].isAssignableFrom(WebSocket.class);
    }

    private static boolean methodSignatureValidForConnectEventHandler(Method m) {
        return m.getReturnType() == void.class &&
                m.getParameterTypes().length == 1 &&
                m.getParameterTypes()[0].isAssignableFrom(WebSocket.class);
    }

    private static boolean methodSignatureValidForErrorEventHandler(Method m) {
        return m.getReturnType() == void.class &&
                m.getParameterTypes().length == 2 &&
                m.getParameterTypes()[0].isAssignableFrom(WebSocket.class) &&
                m.getParameterTypes()[1].isAssignableFrom(Throwable.class);
    }

    private static boolean methodSignatureValidForCloseEventHandler(Method m) {
        return m.getReturnType() == void.class &&
                m.getParameterTypes().length == 2 &&
                m.getParameterTypes()[0].isAssignableFrom(WebSocket.class) &&
                m.getParameterTypes()[1].isAssignableFrom(CloseReason.class);
    }

    private static String getDestinationFrom(Annotation annotation) throws Exception {
        Method valueProvider = annotation.getClass().getDeclaredMethod("value");
        return (String)valueProvider.invoke(annotation);
    }

    private static boolean getCaptureTimings(Annotation annotation) throws Exception {
        Method valueProvider = annotation.getClass().getDeclaredMethod("captureTimings");
        return (boolean) valueProvider.invoke(annotation);
    }

    private static boolean getLogInvocations(Annotation annotation) throws Exception {
        Method valueProvider = annotation.getClass().getDeclaredMethod("logInvocations");
        return (boolean) valueProvider.invoke(annotation);
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
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", ")))
                .append(')');
        return sb.toString();
    }

}
