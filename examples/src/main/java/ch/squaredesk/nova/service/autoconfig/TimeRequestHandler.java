/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.service.autoconfig;


import ch.squaredesk.nova.autoconfigure.service.ServiceDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
public class TimeRequestHandler {
    public final String messagePrefix;

    public TimeRequestHandler(@Autowired(required = false) ServiceDescriptor serviceDescriptor) {
        this.messagePrefix = Optional.ofNullable(serviceDescriptor)
                                    .map(sd -> sd.serviceName + "." + sd.instanceId + " says ")
                                    .orElse("The unnamed says ");
    }

    @GetMapping("/time")
    public String time() {
        return messagePrefix + " " + LocalDateTime.now() + "\n";
    }
}
