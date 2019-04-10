package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class})
public class RestTestConfig {
    @Bean("httpServerPort")
    int httpServerPort() {
        return PortFinder.findFreePort();
    }
//
//    @Bean("restPackagesToScanForHandlers")
//    public String[] restPackagesToScanForHandlers() {
//        return new String[]{"ch.squaredesk.nova.comm.rest"};
//    }
}
