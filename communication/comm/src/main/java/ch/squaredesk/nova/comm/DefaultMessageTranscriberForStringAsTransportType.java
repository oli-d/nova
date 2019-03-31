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

import ch.squaredesk.nova.comm.retrieving.IncomingMessageTranscriber;
import ch.squaredesk.nova.comm.sending.OutgoingMessageTranscriber;

import java.lang.reflect.Method;
import java.math.BigDecimal;

public class DefaultMessageTranscriberForStringAsTransportType extends MessageTranscriber<String> {
    private static final Object objectMapper = instantiateObjectMapperViaReflection();

    public DefaultMessageTranscriberForStringAsTransportType() {
        super(getDefaultOutgoingMessageTranscriber(), getDefaultIncomingMessageTranscriber());

        registerClassSpecificTranscribers(String.class, s -> s, s -> s);
        registerClassSpecificTranscribers(Integer.class, String::valueOf, Integer::parseInt);
        registerClassSpecificTranscribers(Long.class, String::valueOf, Long::parseLong);
        registerClassSpecificTranscribers(Double.class, String::valueOf, Double::parseDouble);
        registerClassSpecificTranscribers(BigDecimal.class, String::valueOf, BigDecimal::new);
    }

    private static OutgoingMessageTranscriber<String> getDefaultOutgoingMessageTranscriber() {
        OutgoingMessageTranscriber<String> returnValue = null;
        if (objectMapper != null) {
            Method marshallingMethodUsingObjectMapper = getMarshallingMethod(objectMapper);
            returnValue = new OutgoingMessageTranscriber<String>() {
                @Override
                public <U> String transcribeOutgoingMessage(U anObject) throws Exception {
                    return (String) marshallingMethodUsingObjectMapper.invoke(objectMapper, anObject);
                }
            } ;
        }
        return returnValue;
    }

    private static IncomingMessageTranscriber<String> getDefaultIncomingMessageTranscriber() {
        IncomingMessageTranscriber<String> returnValue = null;
        if (objectMapper != null) {
            Method unmarshallingMethod = getUnmarshallingMethod(objectMapper);
            returnValue = new IncomingMessageTranscriber<String>() {
                @Override
                public <U> U transcribeIncomingMessage(String anObject, Class<U> typeToUnmarshalTo) throws Exception {
                    return (U) unmarshallingMethod.invoke(objectMapper, anObject, typeToUnmarshalTo);
                }
            };
        }
        return returnValue;
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
}
