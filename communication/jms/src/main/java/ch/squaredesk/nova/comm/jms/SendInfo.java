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

public record SendInfo (
    String correlationId,
    Destination replyDestination,
    Integer deliveryMode,
    Integer priority,
    Long timeToLive,
    Map<String, Object> customHeaders
){
    public SendInfo(
            String correlationId,
            Destination replyDestination,
            Integer deliveryMode,
            Integer priority,
            Long timeToLive
    ) {
        this(correlationId, replyDestination, deliveryMode, priority, timeToLive, null);
    }
}
