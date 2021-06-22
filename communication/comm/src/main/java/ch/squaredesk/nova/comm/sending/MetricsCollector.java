/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class MetricsCollector {
    private final String identifierPrefix;
    private final Counter totalNumberOfSentMessages;

    MetricsCollector(String identifier) {
        this.identifierPrefix =
                (identifier == null || identifier.isBlank() ? "" : identifier.trim() + ".") +
                        "messageSender.sent.";
        totalNumberOfSentMessages = Metrics.counter(this.identifierPrefix + "total");
    }


    public void messageSent(Object destination) {
        Metrics.counter(identifierPrefix + String.valueOf(destination)).increment();
        totalNumberOfSentMessages.increment();
    }

}
