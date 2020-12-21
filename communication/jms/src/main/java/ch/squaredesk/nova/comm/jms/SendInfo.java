/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import javax.jms.Destination;
import java.util.Map;

public class SendInfo {
    public final String correlationId;
    public final Destination replyDestination;
    public final Integer deliveryMode;
    public final Integer priority;
    public final Long timeToLive;
    public final Map<String, Object> customHeaders;

    SendInfo(String correlationId, Destination replyDestination, Map<String, Object> customHeaders,
             Integer deliveryMode, Integer priority, Long timeToLive) {
        this.correlationId = correlationId;
        this.replyDestination = replyDestination;
        this.customHeaders = customHeaders;
        this.deliveryMode = deliveryMode;
        this.priority = priority;
        this.timeToLive = timeToLive;
    }

    @Override
    public String toString() {
        return "JmsSpecificInfo{" +
                "correlationId='" + correlationId + '\'' +
                ", replyDestination=" + replyDestination +
                ", deliveryMode=" + deliveryMode +
                ", priority=" + priority +
                ", timeToLive=" + timeToLive +
                ", customHeaders=" + customHeaders +
                '}';
    }
}
