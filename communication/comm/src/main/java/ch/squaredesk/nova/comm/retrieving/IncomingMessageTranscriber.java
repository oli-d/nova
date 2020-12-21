/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.retrieving;

import io.reactivex.rxjava3.functions.Function;

@FunctionalInterface
public interface IncomingMessageTranscriber<T> {
    <U> U transcribeIncomingMessage (T anObject, Class<U> typeToUnmarshalTo) throws Exception ;

    default <U> Function<T, U> castToFunction(Class<U> requestType) {
        return aMessage -> transcribeIncomingMessage(aMessage, requestType);
    }
}
