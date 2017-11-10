/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class HttpServerConfigurationProvidingConfiguration {
    @Autowired
    Environment environment;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    Nova nova;

    @Bean("httpServerPort")
    public Integer httpServerPort() {
        return environment.getProperty("NOVA.HTTP.SERVER.PORT", Integer.class, 10000);
    }

    @Bean("httpServerInterfaceName")
    public String interfaceName() {
        return environment.getProperty("NOVA.HTTP.SERVER.INTERFACE_NAME", "0.0.0.0");
    }

    @Bean("httpServerConfiguration")
    public HttpServerConfiguration httpServerConfiguration() {
        return new HttpServerConfiguration(interfaceName(), httpServerPort());
    }
}
