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

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultMarshallerFactory {
    public static MessageMarshaller<?, String> getMarshallerForMessageType(Class<?> messageType) {
        if (messageType.isAssignableFrom(String.class)) {
            return object -> object == null ? null : String.valueOf(object);
        } else {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper::writeValueAsString;
        }
    }

    public static MessageUnmarshaller<String, ?> getUnmarshallerForMessageType(Class<?> messageType) {
        if (messageType.isAssignableFrom(String.class)) {
            return s -> s;
        } else {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return s -> objectMapper.readValue((String) s, messageType);
        }
    }

}
