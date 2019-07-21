/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.jms;

import javax.jms.Destination;
import java.util.Map;

public class RetrieveInfo {
    public final String correlationId;
    public final Destination replyDestination;
    public final Map<String, Object> customHeaders;

    RetrieveInfo(String correlationId, Destination replyDestination, Map<String, Object> customHeaders) {
        this.correlationId = correlationId;
        this.replyDestination = replyDestination;
        this.customHeaders = customHeaders;
    }

    public boolean isRpcReply() {
        return correlationId != null && replyDestination==null;
    }

    @Override
    public String toString() {
        return "{" +
                "correlationId='" + correlationId + '\'' +
                ", replyDestination=" + replyDestination +
                ", customHeaders=" + customHeaders +
                '}';
    }
}
