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
import io.reactivex.Completable;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageSenderTest {

    @Test
    void instanceCannotBeCreatedWithoutMarshaller() {
        Throwable t = assertThrows(NullPointerException.class,
                () -> new MessageSender<Object,Object, Object, OutgoingMessageMetaData<Object, Object>>(null, new Metrics()) {
                    @Override
                    protected Completable doSend(Object transportMessage, OutgoingMessageMetaData messageSendingInfo) {
                        return null;
                    }
                });
        assertThat(t.getMessage(), containsString("messageMarshaller"));
    }

}
