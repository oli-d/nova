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
        return getSpecificOutgoingTranscriber(typeToMarshal);
    }

    public <T> Function<TransportMessageType, T> getIncomingMessageTranscriber(Class<T> typeToUnmarshalTo) {
        return getSpecificIncomingTranscriber(typeToUnmarshalTo);
    }

    protected <T> Function<T, TransportMessageType> createDefaultOutgoingMessageTranscriberFor(Class<T> typeToMarshal) {
        if (defaultOutgoingMessageTranscriber != null) {
            return defaultOutgoingMessageTranscriber.castToFunction(typeToMarshal);
        } else {
            throw new IllegalArgumentException("Unable to create an OutgingMessageTranscriber for class " + typeToMarshal);
        }
    }

    protected <T> Function<TransportMessageType, T> createDefaultIncomingMessageTranscriberFor(Class<T> typeToUnmarshalTo) {
        if (defaultIncomingMessageTranscriber != null) {
            return defaultIncomingMessageTranscriber.castToFunction(typeToUnmarshalTo);
        } else {
            throw new IllegalArgumentException("Unable to create an IncomingMessageTranscriber for class " + typeToUnmarshalTo);
        }
    }

    private <T> Function<T, TransportMessageType> getSpecificOutgoingTranscriber (Class<T> targetClass) {
        return (Function<T, TransportMessageType>) specificOutgoingTranscribers.computeIfAbsent(
                targetClass,
                theClass -> createDefaultOutgoingMessageTranscriberFor(theClass)
        );
    }

    private <T> Function<TransportMessageType, T> getSpecificIncomingTranscriber (Class<T> targetClass) {
        return (Function<TransportMessageType, T>) specificIncomingTranscribers.computeIfAbsent(
                targetClass,
                theClass -> createDefaultIncomingMessageTranscriberFor(theClass)
        );
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
