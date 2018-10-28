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
import io.reactivex.functions.Function;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageTranscriber<TransportMessageType>  {
    private Map<Class<?>, Function<?, TransportMessageType>> specificOutgoingTranscribers = new ConcurrentHashMap<>();
    private Map<Class<?>, Function<TransportMessageType, ?>> specificIncomingTranscribers = new ConcurrentHashMap<>();
    private OutgoingMessageTranscriber<TransportMessageType> defaultOutgoingMessageTranscriber;
    private IncomingMessageTranscriber<TransportMessageType> defaultIncomingMessageTranscriber;

    public MessageTranscriber(OutgoingMessageTranscriber<TransportMessageType> defaultOutgoingMessageTranscriber,
                              IncomingMessageTranscriber<TransportMessageType> defaultIncomingMessageTranscriber) {
        this.defaultIncomingMessageTranscriber = defaultIncomingMessageTranscriber;
        this.defaultOutgoingMessageTranscriber = defaultOutgoingMessageTranscriber;
    }

    public <T> Function<T, TransportMessageType> getOutgoingMessageTranscriber(T anObject) {
        if (anObject == null) {
            return null;
        }
        Class<T> specificType = (Class<T>) anObject.getClass();
        return getOutgoingMessageTranscriber(specificType);
    }

    public <T> Function<T, TransportMessageType> getOutgoingMessageTranscriber(Class<T> typeToMarshal) {
        Function<T, TransportMessageType> specificTranscriber = getSpecificOutgoingTranscriber(typeToMarshal);
        if (specificTranscriber != null) {
            return specificTranscriber;
        } else if (defaultOutgoingMessageTranscriber != null) {
            return defaultOutgoingMessageTranscriber.castToFunction(typeToMarshal);
        } else {
            throw new IllegalArgumentException("Unable to find an appropriate message transcriber");
        }
    }

    public <T> Function<TransportMessageType, T> getIncomingMessageTranscriber(Class<T> typeToUnmarshalTo) {
        Function<TransportMessageType, T> specificTranscriber = getSpecificIncomingTranscriber(typeToUnmarshalTo);
        if (specificTranscriber != null) {
            return specificTranscriber;
        } else if (defaultIncomingMessageTranscriber != null) {
            return defaultIncomingMessageTranscriber.castToFunction(typeToUnmarshalTo);
        } else {
            throw new IllegalArgumentException("Unable to find an appropriate message transcriber");
        }
    }

    private <T> Function<T, TransportMessageType> getSpecificOutgoingTranscriber (Class<T> targetClass) {
        return (Function<T, TransportMessageType>) specificOutgoingTranscribers.get(targetClass);
    }

    private <T> Function<TransportMessageType, T> getSpecificIncomingTranscriber (Class<T> targetClass) {
        return (Function<TransportMessageType, T>) specificIncomingTranscribers.get(targetClass);
    }

    public <T> void registerClassSpecificTranscribers (Class<T> targetClass,
                                                   Function<T, TransportMessageType> outgoingMessageTranscriber,
                                                   Function<TransportMessageType, T> incomingMessageTranscriber) {
        if (targetClass.equals(Object.class)) {
            // Does not work, since the call objectMapper.readValue(string, Object.class) will always return a String.
            throw new IllegalArgumentException("unmarshaller for class java.lang.Object is not supported");
        }

        if (incomingMessageTranscriber == null) {
            specificIncomingTranscribers.remove(targetClass);
        } else {
            specificIncomingTranscribers.put(targetClass, incomingMessageTranscriber);
        }
        if (outgoingMessageTranscriber == null) {
            specificOutgoingTranscribers.remove(targetClass);
        } else {
            specificOutgoingTranscribers.put(targetClass, outgoingMessageTranscriber);
        }
    }
}
