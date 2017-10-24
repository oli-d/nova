package ch.squaredesk.nova.comm.http.annotation.example;

import ch.squaredesk.nova.comm.http.annotation.HttpEnablingConfiguration;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({HttpEnablingConfiguration.class, NovaProvidingConfiguration.class })
public class EchoConfiguration {
    @Bean
    public EchoHandler echoHandler() {
        return new EchoHandler();
    }
}
