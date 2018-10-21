/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;

import ch.squaredesk.nova.comm.MarshallerRegistry;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageSenderImplBaseTest {

    @Test
    void instanceCanBeCreatedWithoutMarshallerProvider() {
        MessageSenderImplBase<Object, Object, OutgoingMessageMetaData<Object, Object>> impl =
                new MessageSenderImplBase<Object, Object, OutgoingMessageMetaData<Object, Object>>(null, new Metrics()) {
                    @Override
                    public <T> Completable doSend(T message, MessageMarshaller<T, Object> marshaller, OutgoingMessageMetaData<Object, Object> outgoingMessageMetaData) {
                        return null;
                    }
                }
        ;
        assertNotNull(impl);
    }

    @Test
    void instanceCannotBeCreatedWithoutMetrics() {
        MarshallerRegistry<Object> marshallerRegistry = new MarshallerRegistry<Object>() {
            @Override
            public <U> MessageUnmarshaller<Object, U> getUnmarshallerForMessageType(Class<U> tClass) {
                return null;
            }
            @Override
            public <T> MessageMarshaller<T, Object> getMarshallerForMessageType(Class<T> tClass) {
                return null;
            }
        };
        Throwable t = assertThrows(NullPointerException.class,
                () -> {
                    new MessageSenderImplBase<Object, Object, OutgoingMessageMetaData<Object, Object>>(marshallerRegistry, null) {
                        @Override
                        public <T> Completable doSend(T message, MessageMarshaller<T, Object> marshaller, OutgoingMessageMetaData<Object, Object> outgoingMessageMetaData) {
                            return null;
                        }
                    };
                });
        assertThat(t.getMessage(), containsString("metrics"));
    }

}
