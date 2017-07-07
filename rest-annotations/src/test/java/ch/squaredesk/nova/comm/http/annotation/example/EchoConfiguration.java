package ch.squaredesk.nova.comm.http.annotation.example;

import ch.squaredesk.nova.comm.http.annotation.RestServerProvidingConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(RestServerProvidingConfiguration.class)
public class EchoConfiguration {
    @Bean
    public EchoHandler echoHandler() {
        return new EchoHandler();
    }
}
