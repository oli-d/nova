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
package ch.squaredesk.nova.service.autoconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class TimeServiceConfig  {
    @Bean
    public TimeService serviceInstance() {
        return new TimeService();
    }

    @Bean
    public TimeRequestHandler timeRequestHandler(Environment env) {
        return new TimeRequestHandler(env.getProperty("messagePrefix",""));
    }
}
