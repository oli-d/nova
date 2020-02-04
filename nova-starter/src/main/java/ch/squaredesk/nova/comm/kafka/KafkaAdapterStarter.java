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
package ch.squaredesk.nova.comm.kafka;

import org.springframework.beans.factory.DisposableBean;

public class KafkaAdapterStarter implements DisposableBean {
    private final KafkaAdapter kafkaAdapter;

    public KafkaAdapterStarter(KafkaAdapter kafkaAdapter) {
        this.kafkaAdapter = kafkaAdapter;
    }

    @Override
    public void destroy() throws Exception {
        try {
            kafkaAdapter.shutdown();
        } catch (Exception e) {
            // noop; shutdown anyway...
        }
    }
}
