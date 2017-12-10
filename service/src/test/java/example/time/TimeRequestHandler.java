/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package example.time;

import ch.squaredesk.nova.comm.rest.annotation.OnRestRequest;

import java.time.LocalDateTime;

public class TimeRequestHandler {
    public final String messagePrefix;

    public TimeRequestHandler(String messagePrefix) {
        this.messagePrefix = messagePrefix;
    }

    @OnRestRequest("/time")
    public String time() {
        return messagePrefix + " " + LocalDateTime.now() + "\n";
    }
}
