/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm;

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomingMessageMetaDataTest {
    @Test
    void testBuilder() {
        int destination = 1;
        Map<String, Object> customHeaders = new HashMap<>();

        IncomingMessageMetaData<Integer, Map<String, Object>> md = new IncomingMessageMetaData.Builder<Integer, Map<String, Object>>()
                .withDestination(destination)
                .withTransportSpecificDetails(customHeaders)
                .build();

        assertTrue(md.transportSpecificDetails == customHeaders);
        assertTrue(md.origin == destination);
    }
}
