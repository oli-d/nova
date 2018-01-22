/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.rest.annotation.example;

import ch.squaredesk.nova.comm.rest.annotation.RestEnablingConfiguration;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class })
public class EchoConfiguration {
    @Bean
    public EchoHandler echoHandler() {
        return new EchoHandler();
    }
}
