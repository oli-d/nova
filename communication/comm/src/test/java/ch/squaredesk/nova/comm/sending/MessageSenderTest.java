/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;

import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessageSenderTest {

    @Test
    void instanceCanBeCreatedWithoutMarshallerProvider() {
        MessageSender<Object, Object, OutgoingMessageMetaData<Object, Object>> impl =
                new MessageSender<Object, Object, OutgoingMessageMetaData<Object, Object>>(null) {
                    @Override
                    public Single<OutgoingMessageMetaData<Object, Object>> send(Object message, OutgoingMessageMetaData<Object, Object> sendingInfo) {
                        return null;
                    }
                }
        ;
        assertNotNull(impl);
    }
}
