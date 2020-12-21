/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm;

import ch.squaredesk.nova.comm.retrieving.IncomingMessageTranscriber;
import ch.squaredesk.nova.comm.sending.OutgoingMessageTranscriber;
import io.reactivex.rxjava3.functions.Function;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageTranscriber<T>  {
    private Map<Class<?>, Function<?, T>> specificOutgoingTranscribers = new ConcurrentHashMap<>();
    private Map<Class<?>, Function<T, ?>> specificIncomingTranscribers = new ConcurrentHashMap<>();
    private OutgoingMessageTranscriber<T> defaultOutgoingMessageTranscriber;
    private IncomingMessageTranscriber<T> defaultIncomingMessageTranscriber;

    public MessageTranscriber(OutgoingMessageTranscriber<T> defaultOutgoingMessageTranscriber,
                              IncomingMessageTranscriber<T> defaultIncomingMessageTranscriber) {
        this.defaultIncomingMessageTranscriber = defaultIncomingMessageTranscriber;
        this.defaultOutgoingMessageTranscriber = defaultOutgoingMessageTranscriber;
    }

    public <U> Function<U, T> getOutgoingMessageTranscriber(U anObject) {
        if (anObject == null) {
            return null;
        }
        Class<U> specificType = (Class<U>) anObject.getClass();
        return getOutgoingMessageTranscriber(specificType);
    }

    public <U> Function<U, T> getOutgoingMessageTranscriber(Class<U> typeToMarshal) {
        return getSpecificOutgoingTranscriber(typeToMarshal);
    }

    public <U> Function<T, U> getIncomingMessageTranscriber(Class<U> typeToUnmarshalTo) {
        return getSpecificIncomingTranscriber(typeToUnmarshalTo);
    }

    protected <U> Function<U, T> createDefaultOutgoingMessageTranscriberFor(Class<U> typeToMarshal) {
        if (defaultOutgoingMessageTranscriber != null) {
            return defaultOutgoingMessageTranscriber.castToFunction(typeToMarshal);
        } else {
            throw new IllegalArgumentException("Unable to create an OutgingMessageTranscriber for class " + typeToMarshal);
        }
    }

    protected <U> Function<T, U> createDefaultIncomingMessageTranscriberFor(Class<U> typeToUnmarshalTo) {
        if (defaultIncomingMessageTranscriber != null) {
            return defaultIncomingMessageTranscriber.castToFunction(typeToUnmarshalTo);
        } else {
            throw new IllegalArgumentException("Unable to create an IncomingMessageTranscriber for class " + typeToUnmarshalTo);
        }
    }

    private <U> Function<U, T> getSpecificOutgoingTranscriber (Class<U> targetClass) {
        return (Function<U, T>) specificOutgoingTranscribers.computeIfAbsent(
                targetClass,
                this::createDefaultOutgoingMessageTranscriberFor
        );
    }

    private <U> Function<T, U> getSpecificIncomingTranscriber (Class<U> targetClass) {
        return (Function<T, U>) specificIncomingTranscribers.computeIfAbsent(
                targetClass,
                this::createDefaultIncomingMessageTranscriberFor
        );
    }

    public <U> void registerClassSpecificTranscribers (Class<U> targetClass,
                                                       Function<U, T> outgoingMessageTranscriber,
                                                       Function<T, U> incomingMessageTranscriber) {
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
