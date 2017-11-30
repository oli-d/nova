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

package ch.squaredesk.nova.comm;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;

import java.lang.reflect.Method;
import java.math.BigDecimal;

public class DefaultMarshallerFactory {
    public static MessageMarshaller<?, String> getMarshallerForMessageType(Class<?> messageType) {
        if (messageType.equals(Object.class)) {
            // Jackson obviously can transfer arbitrary objects into a String, so we would not need to
            // throw an exception here. Unfortunately unmarshalling would not work, since the
            // call objectMapper.readValue(string, Object.class) will always return a String.
            // Since we want to be "symmetric", we therefore also don't allow default marshaller for Object.class
            throw new IllegalArgumentException("default marshaller for class java.lang.Object is not supported");
        } else if (String.class.isAssignableFrom(messageType)) {
            return object -> object == null ? null : String.valueOf(object);
        } else if (Integer.class.isAssignableFrom(messageType)) {
            return String::valueOf;
        } else if (Long.class.isAssignableFrom(messageType)) {
            return String::valueOf;
        } else if (Double.class.isAssignableFrom(messageType)) {
            return String::valueOf;
        } else if (BigDecimal.class.isAssignableFrom(messageType)) {
            return String::valueOf;
        } else {
            Object objectMapper = instantiateObjectMapperViaReflection();
            if (objectMapper==null) {
                throw new IllegalArgumentException("Unable to create default marshaller for message type " + messageType);
            } else {
                Method marshallingMethod = getMarshallingMethod(objectMapper);
                return new MessageMarshaller<Object, String>() {
                    @Override
                    public String marshal(Object o) throws Exception {
                        return (String)marshallingMethod.invoke(objectMapper, o);
                    }
                };
            }
        }
    }

    public static MessageUnmarshaller<String, ?> getUnmarshallerForMessageType(Class<?> messageType) {
        if (messageType.equals(Object.class)) {
            // Does not work, since the call objectMapper.readValue(string, Object.class) will always return a String.
            throw new IllegalArgumentException("default unmarshaller for class java.lang.Object is not supported");
        } else if (String.class.isAssignableFrom(messageType)) {
            return s -> s;
        } else if (Integer.class.isAssignableFrom(messageType)) {
            return Integer::parseInt;
        } else if (Long.class.isAssignableFrom(messageType)) {
            return Long::parseLong;
        } else if (Double.class.isAssignableFrom(messageType)) {
            return Double::parseDouble;
        } else if (BigDecimal.class.isAssignableFrom(messageType)) {
            return BigDecimal::new;
        } else {
            Object objectMapper = instantiateObjectMapperViaReflection();
            if (objectMapper == null) {
                throw new IllegalArgumentException("Unable to create default unmarshaller for message type " + messageType);
            } else {
                Method unmarshallingMethod = getUnmarshallingMethod(objectMapper);
                return new MessageUnmarshaller<String, Object>() {
                    @Override
                    public Object unmarshal(String s) throws Exception {
                        return unmarshallingMethod.invoke(objectMapper, s, messageType);
                    }
                };
            }
        }
    }

    private static Object instantiateObjectMapperViaReflection() {
        try {
            Class objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            Object objectMapper = objectMapperClass.newInstance();

            Class deserializationFeatureClass = Class.forName("com.fasterxml.jackson.databind.DeserializationFeature");
            Object deserializationFeature = Enum.valueOf(deserializationFeatureClass, "FAIL_ON_UNKNOWN_PROPERTIES");

            Method serializationConfigMethod = objectMapperClass.getMethod(
                    "configure", deserializationFeatureClass, boolean.class);
            serializationConfigMethod.invoke(objectMapper, deserializationFeature, false);

            return objectMapper;
        } catch (Exception e) {
            return null;
        }
    }

    private static Method getMarshallingMethod(Object objectMapper) {
        try {
            return objectMapper.getClass().getMethod("writeValueAsString", Object.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find unmarshalling method in ObjectMapper", e);
        }
    }

    private static Method getUnmarshallingMethod(Object objectMapper) {
        try {
            return objectMapper.getClass().getMethod("readValue", String.class, Class.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find unmarshalling method in ObjectMapper", e);
        }
    }

}
