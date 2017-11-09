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

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.sort;
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
//                .peek(method -> {
//                    if (!Modifier.isPublic(method.getModifiers()))
//                        throw new IllegalArgumentException(
//                                "Method " + prettyPrint(bean, method) + ", annotated with @" +
//                                        OnMessage.class.getSimpleName() + " must be public");
//                })
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

    static boolean methodSignatureValidForMessageHandler (Method m) {
        return m.getReturnType() == void.class &&
                m.getParameterTypes().length == 2 &&
                m.getParameterTypes()[1].isAssignableFrom(WebSocket.class);
    }

    private static MessageMarshaller instantiateMarshaller(Method method, OnMessage annotation) {
        MessageMarshaller marshaller = null;
        if (annotation.messageMarshallerClassName()!=null
                && !annotation.messageMarshallerClassName().isEmpty()) {
            marshaller = instantiateMarshaller(method, annotation.messageMarshallerClassName());
        } else {
            marshaller = getDefaultMarshaller(method);
        }

        return marshaller;
    }

    private static MessageUnmarshaller instantiateUnmarshaller(Method method, OnMessage annotation)  {
        MessageUnmarshaller unmarshaller = null;
        if (annotation.messageUnmarshallerClassName()!=null
                && !annotation.messageUnmarshallerClassName().isEmpty()) {
            unmarshaller = instantiateUnmarshaller(method, annotation.messageUnmarshallerClassName());
        } else {
            unmarshaller = getDefaultUnmarshaller(method);
        }

        return unmarshaller;
    }

    /**
     * Since Lambdas do erase type information, the method only works if the passed marshaller is NOT a lambda!!!
     *
     * See: https://stackoverflow.com/questions/21887358/reflection-type-inference-on-java-8-lambdas
     */
    static boolean marshallerAcceptsType(MessageMarshaller<?,String> marshaller, Class paramTypeToCheckFor) {
        Type genericInterfaceType = Arrays.stream(marshaller.getClass().getGenericInterfaces())
                .filter(t -> t.getTypeName().startsWith(MessageMarshaller.class.getName()))
                .findFirst()
                .get();


        if (genericInterfaceType instanceof ParameterizedType) {
            Type inputType = ((ParameterizedType)genericInterfaceType).getActualTypeArguments()[0];
            return (inputType instanceof Class) && ((Class) inputType).isAssignableFrom(paramTypeToCheckFor);
        }

        return false;
    }

    /**
     * Since Lambdas do erase type information, the method only works if the passed unmarshaller is NOT a lambda!!!
     *
     * See: https://stackoverflow.com/questions/21887358/reflection-type-inference-on-java-8-lambdas
     */
    static boolean unmarshallerReturnsType(MessageUnmarshaller unmarshaller, Class returnTypeToCheckFor) {
        Type genericInterfaceType = Arrays.stream(unmarshaller.getClass().getGenericInterfaces())
                .filter(t -> t.getTypeName().startsWith(MessageUnmarshaller.class.getName()))
                .findFirst()
                .get();


        if (genericInterfaceType instanceof ParameterizedType) {
            Type inputType = ((ParameterizedType)genericInterfaceType).getActualTypeArguments()[1];
            return (inputType instanceof Class) && ((Class) inputType).isAssignableFrom(returnTypeToCheckFor);
        }

        return false;
    }

    private static MessageMarshaller getDefaultMarshaller(Method method) {
        if (method.getParameterTypes()[0].isAssignableFrom(String.class)) {
            return s -> s;
        } else {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return o -> objectMapper.writeValueAsString(o);
        }
    }

    private static MessageUnmarshaller getDefaultUnmarshaller(Method method) {
        if (method.getParameterTypes()[0].isAssignableFrom(String.class)) {
            return s -> s;
        } else {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return s -> objectMapper.readValue((String)s, method.getParameterTypes()[0]);
        }
    }

    private static MessageMarshaller instantiateMarshaller(Method method, String className) {
        Class classObject = null;
        try {
            classObject = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load class " + className);
        }
        if (!classObject.isAssignableFrom(MessageMarshaller.class)) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageMarshaller");
        }
        MessageMarshaller messageMarshaller;
        try {
            messageMarshaller = (MessageMarshaller) classObject.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to instantiate unmarshaller " + className);
        }
        if (!marshallerAcceptsType(messageMarshaller, method.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageMarshaller for method "
                    + method.getName());
        }
        return messageMarshaller;
    }

    private static MessageUnmarshaller instantiateUnmarshaller(Method method, String className)  {
        Class classObject;
        try {
            classObject = Class.forName(className);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load class " + className);
        }
        if (!classObject.isAssignableFrom(MessageUnmarshaller.class)) {
            throw new IllegalArgumentException("Class " + className + " is not a valid MessageUnmarshaller");
        }
        MessageUnmarshaller messageUnmarshaller;
        try {
            messageUnmarshaller = (MessageUnmarshaller) classObject.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to instantiate unmarshaller " + className);
        }
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
