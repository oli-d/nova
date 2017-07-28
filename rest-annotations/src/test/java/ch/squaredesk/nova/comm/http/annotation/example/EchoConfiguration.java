package ch.squaredesk.nova.comm.http.annotation.example;

import ch.squaredesk.nova.comm.http.annotation.RestEnablingConfiguration;
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
