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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultMarshallerRegistryForStringAsTransportType implements MarshallerRegistry<String> {
    private Map<Class<?>, MessageMarshaller<?, String>> typeToMarshaller = new HashMap<>();
    private Map<Class<?>, MessageUnmarshaller<String, ?>> typeToUnmarshaller = new HashMap<>();
    private final Object objectMapper;

    public DefaultMarshallerRegistryForStringAsTransportType() {
        typeToMarshaller.put(String.class, object -> object == null ? null : String.valueOf(object));
        typeToMarshaller.put(Integer.class, String::valueOf);
        typeToMarshaller.put(Long.class, String::valueOf);
        typeToMarshaller.put(Double.class, String::valueOf);
        typeToMarshaller.put(BigDecimal.class, String::valueOf);

        typeToUnmarshaller.put(String.class, s -> s);
        typeToUnmarshaller.put(Integer.class, Integer::parseInt);
        typeToUnmarshaller.put(Long.class, Long::parseLong);
        typeToUnmarshaller.put(Double.class, Double::parseDouble);
        typeToUnmarshaller.put(BigDecimal.class, BigDecimal::new);

        objectMapper = instantiateObjectMapperViaReflection();
    }

    @Override
    public <MessageType> MessageMarshaller<MessageType, String> getMarshallerForMessageType(Class<MessageType> messageType) {
        MessageMarshaller<MessageType, String> returnValue = (MessageMarshaller<MessageType, String>) typeToMarshaller.get(messageType);

        returnValue = Optional.ofNullable(returnValue)
                .orElseGet(() -> {
                    // TODO: should we update the map to speedup the next lookup?
                    if (objectMapper != null) {
                        Method marshallingMethod = getMarshallingMethod(objectMapper);
                        return o -> {
                            try {
                                return (String) marshallingMethod.invoke(objectMapper, o);
                            } catch (Exception e) {
                                throw new RuntimeException("Unable to marshal outgoing message " + o, e);
                            }
                        };
                    } else {
                        return null;
                    }
                });

        return Optional.ofNullable(returnValue)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find marshaller for message type " + messageType));
    }

    @Override
    public <T> MessageUnmarshaller<String, T> getUnmarshallerForMessageType(Class<T> messageType) {
        if (messageType.equals(Object.class)) {
            // Does not work, since the call objectMapper.readValue(string, Object.class) will always return a String.
            throw new IllegalArgumentException("unmarshaller for class java.lang.Object is not supported");
        }

        MessageUnmarshaller<String, T> returnValue = (MessageUnmarshaller<String, T>) typeToUnmarshaller.get(messageType);

        returnValue = Optional.ofNullable(returnValue)
                .orElseGet(() -> {
                    // TODO: should we update the map to speedup the next lookup?
                    if (objectMapper != null) {
                        Method unmarshallingMethod = getUnmarshallingMethod(objectMapper);
                        return s -> {
                            try {
                                return (T) unmarshallingMethod.invoke(objectMapper, s, messageType);
                            } catch (Exception e) {
                                throw new RuntimeException("Unable to unmarshal incoming message '" + s + "'", e);
                            }
                        };
                    } else {
                        return null;
                    }
                });

        return Optional.ofNullable(returnValue)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find unmarshaller for message type " + messageType));
    }

    private static Object instantiateObjectMapperViaReflection() {
        try {
            Class objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            Object objectMapper = objectMapperClass.getConstructor().newInstance();

            Class deserializationFeatureClass = Class.forName("com.fasterxml.jackson.databind.DeserializationFeature");
            Object deserializationFeature = Enum.valueOf(deserializationFeatureClass, "FAIL_ON_UNKNOWN_PROPERTIES");

            Method serializationConfigMethod = objectMapperClass.getMethod(
                    "configure", deserializationFeatureClass, boolean.class);
            serializationConfigMethod.invoke(objectMapper, deserializationFeature, false);

            /**
             * WARNING - As of Jackson 2.x, auto-registration will only register older JSR310Module, and not newer
             * JavaTimeModule -- this is due to backwards compatibility. Because of this make sure to either use
             * explicit registration, or, if you want to use JavaTimeModule but also auto-registration, make sure
             * to register JavaTimeModule BEFORE calling mapper.findAndRegisterModules()).
             *
             * Jackson 3.x changes things as it requires Java 8 to work and can thereby directly supported features.
             * Because of this parameter-names and datatypes modules are merged into jackson-databind and need not be
             * registered; datetime module (JavaTimeModule) remains separate module due to its size and configurability
             * options.
             */
            Method findAndRegisterModulesMethod = objectMapperClass.getMethod("findAndRegisterModules");
            findAndRegisterModulesMethod.invoke(objectMapper);

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

    public <T> void setDefaultMarshaller(Class<T> objectClass, MessageMarshaller<T, String> marshaller) {
        typeToMarshaller.put(objectClass, marshaller);
    }

    public <T> void setDefaultUnmarshaller(Class<T> objectClass, MessageUnmarshaller<String, T> unmarshaller) {
        typeToUnmarshaller.put(objectClass, unmarshaller);
    }
}
