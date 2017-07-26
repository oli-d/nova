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

import ch.squaredesk.nova.comm.http.annotation.RestServerProvidingConfiguration;
import ch.squaredesk.nova.service.NovaServiceConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

@Configuration
@Import(RestServerProvidingConfiguration.class)
public class TimeServiceConfig extends NovaServiceConfiguration<TimeService> {
    @Autowired
    Environment env;

    @Autowired
    @Lazy
    HttpServer restHttpServer;

    @Override
    @Bean
    public TimeService serviceInstance() {
        return new TimeService(restHttpServer);
    }

    @Bean
    public TimeRequestHandler timeRequestHandler() {
        return new TimeRequestHandler(env.getProperty("messagePrefix",""));
    }
}
