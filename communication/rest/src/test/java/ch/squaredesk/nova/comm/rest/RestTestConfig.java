package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.comm.http.spring.HttpServerProvidingConfiguration;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RestEnablingConfiguration.class})
public class RestTestConfig {
    @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.PORT)
    int httpServerPort() {
        return PortFinder.findFreePort();
    }
}
