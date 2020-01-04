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

package ch.squaredesk.nova.comm.sending;

import io.reactivex.functions.Function;

@FunctionalInterface
public interface OutgoingMessageTranscriber<T> {
    <U> T transcribeOutgoingMessage(U anObject) throws Exception ;

    default <U> Function<U, T> castToFunction(Class<U> requestType) {
        return this::transcribeOutgoingMessage;
    }
}
