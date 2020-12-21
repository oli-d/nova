/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.service.autoconfig;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.time.LocalDateTime;

@Path("/time")
public class TimeRequestHandler {
    public final String messagePrefix;

    public TimeRequestHandler(String messagePrefix) {
        this.messagePrefix = messagePrefix;
    }

    @GET
    public String time() {
        return messagePrefix + " " + LocalDateTime.now() + "\n";
    }
}
