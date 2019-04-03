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

import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class HttpServerConfigurationProvidingConfiguration {
    @Autowired
    Environment environment;

    @Bean("httpServerPort")
    public int httpServerPort() {
        return environment.getProperty("NOVA.HTTP.SERVER.PORT", Integer.class, 10000);
    }

    @Bean("httpServerInterfaceName")
    public String interfaceName() {
        return environment.getProperty("NOVA.HTTP.SERVER.INTERFACE_NAME", "0.0.0.0");
    }

    @Bean("httpServerTrustStore")
    public String sslTrustStorePass() {
        return environment.getProperty("NOVA.HTTP.SERVER.TRUST_STORE");
    }

    @Bean("httpServerTrustStorePass")
    public String sslTrustStorePath() {
        return environment.getProperty("NOVA.HTTP.SERVER.TRUST_STORE_PASS");
    }

    @Bean("httpServerKeyStore")
    public String sslKeyStorePass() {
        return environment.getProperty("NOVA.HTTP.SERVER.KEY_STORE");
    }

    @Bean("httpServerKeyStorePass")
    public String sslKeyStorePath() {
        return environment.getProperty("NOVA.HTTP.SERVER.KEY_STORE_PASS");
    }

    @Bean("httpServerConfiguration")
    public HttpServerConfiguration httpServerConfiguration(
            @Qualifier("httpServerInterfaceName") String interfaceName,
            @Qualifier("httpServerPort") int httpServerPort,
            @Qualifier("httpServerTrustStore")String sslTrustStorePass,
            @Qualifier("httpServerTrustStorePass") String sslTrustStorePath,
            @Qualifier("httpServerKeyStore") String sslKeyStorePass,
            @Qualifier("httpServerKeyStorePass") String sslKeyStorePath) {
        return HttpServerConfiguration.builder()
                .interfaceName(interfaceName())
                .port(httpServerPort())
                .sslKeyStorePath(sslKeyStorePath())
                .sslKeyStorePass(sslKeyStorePass())
                .sslTrustStorePath(sslTrustStorePath())
                .sslTrustStorePass(sslTrustStorePass())
                .build();
    }

}
