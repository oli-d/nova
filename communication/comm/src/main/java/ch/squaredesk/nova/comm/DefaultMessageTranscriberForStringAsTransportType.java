/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultMessageTranscriberForStringAsTransportType extends MessageTranscriber<String> {
    public DefaultMessageTranscriberForStringAsTransportType() {
        this(instantiateObjectMapper());
    }

    public DefaultMessageTranscriberForStringAsTransportType(ObjectMapper objectMapper) {
        super(objectMapper::writeValueAsString, objectMapper::readValue);
        registerClassSpecificTranscribers(String.class, s->s, s->s);
    }

    private static ObjectMapper instantiateObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
