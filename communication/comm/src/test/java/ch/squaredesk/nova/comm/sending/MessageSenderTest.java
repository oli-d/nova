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

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageSenderTest {

    @Test
    void instanceCanBeCreatedWithoutMarshallerProvider() {
        MessageSender<Object, Object, OutgoingMessageMetaData<Object, Object>> impl =
                new MessageSender<Object, Object, OutgoingMessageMetaData<Object, Object>>(null, new Metrics()) {
                    @Override
                    public Single<OutgoingMessageMetaData<Object, Object>> send(Object message, OutgoingMessageMetaData<Object, Object> sendingInfo) {
                        return null;
                    }
                }
        ;
        assertNotNull(impl);
    }

    @Test
    void instanceCannotBeCreatedWithoutMetrics() {
        Throwable t = assertThrows(NullPointerException.class,
                () -> {
                    new MessageSender<Object, Object, OutgoingMessageMetaData<Object, Object>>(null) {
                        @Override
                        public Single<OutgoingMessageMetaData<Object, Object>> send(Object message, OutgoingMessageMetaData<Object, Object> sendingInfo) {
                            return null;
                        }
                    };
                });
        assertThat(t.getMessage(), containsString("metrics"));
    }

}
