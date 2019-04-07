package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.HttpAdapter;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.concurrent.TimeUnit;

@Configuration
@Import({HttpServerProvidingConfiguration.class, NovaProvidingConfiguration.class})
public class HttpEnablingConfiguration {
    @Bean("httpAdapter")
    HttpAdapter httpAdapter(@Qualifier("httpServer") @Autowired(required = false) HttpServer httpServer,
                            @Qualifier("defaultHttpRequestTimeoutInSeconds") int defaultHttpRequestTimeoutInSeconds,
                            @Qualifier("httpAdapterIdentifier") @Autowired(required=false) String defaultHttpAdapterIdentifier,
                            @Qualifier("httpMessageTranscriber") MessageTranscriber<String> httpMessageTranscriber,
                            Nova nova) {
        return HttpAdapter.builder()
                .setDefaultRequestTimeout(defaultHttpRequestTimeoutInSeconds, TimeUnit.SECONDS)
                .setHttpServer(httpServer)
                .setIdentifier(defaultHttpAdapterIdentifier)
                .setMessageTranscriber(httpMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }

    @Bean("defaultHttpRequestTimeoutInSeconds")
    int defaultHttpRequestTimeoutInSeconds(Environment environment) {
        return environment.getProperty("NOVA.HTTP.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS", Integer.class, 30);
    }

    @Bean("httpAdapterIdentifier")
    String defaultHttpAdapterIdentifier(Environment environment) {
        return environment.getProperty("NOVA.HTTP.ADAPTER_IDENTIFIER");
    }

    @Bean("httpMessageTranscriber")
    MessageTranscriber<String> httpMessageTranscriber() {
        return new DefaultMessageTranscriberForStringAsTransportType();
    }
}
