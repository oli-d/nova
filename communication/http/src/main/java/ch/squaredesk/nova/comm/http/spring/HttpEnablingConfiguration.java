package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.HttpAdapter;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public interface BeanIdentifiers {
        String ADAPTER_IDENTIFIER = "NOVA.HTTP.ADAPTER_IDENTIFIER";
        String DEFAULT_REQUEST_TIMEOUT_IN_SECONDS = "NOVA.HTTP.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS";
        String OBJECT_MAPPER = "NOVA.HTTP.OBJECT_MAPPER";
        String MESSAGE_TRANSCRIBER = "NOVA.HTTP.MESSAGE_TRANSCRIBER";

        String ADAPTER_INSTANCE = "NOVA.HTTP.ADAPTER";
    }


    @Bean(BeanIdentifiers.ADAPTER_INSTANCE)
    HttpAdapter httpAdapter(@Qualifier(HttpServerProvidingConfiguration.BeanIdentifiers.SERVER) @Autowired(required = false) HttpServer httpServer,
                            @Qualifier(BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS) int defaultHttpRequestTimeoutInSeconds,
                            @Qualifier(BeanIdentifiers.ADAPTER_IDENTIFIER) @Autowired(required=false) String defaultHttpAdapterIdentifier,
                            @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> httpMessageTranscriber,
                            Nova nova) {
        return HttpAdapter.builder()
                .setDefaultRequestTimeout(defaultHttpRequestTimeoutInSeconds, TimeUnit.SECONDS)
                .setHttpServer(httpServer)
                .setIdentifier(defaultHttpAdapterIdentifier)
                .setMessageTranscriber(httpMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }

    @Bean(BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS)
    int defaultHttpRequestTimeoutInSeconds(Environment environment) {
        return environment.getProperty(BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS, Integer.class, 30);
    }

    @Bean(BeanIdentifiers.ADAPTER_IDENTIFIER)
    String defaultHttpAdapterIdentifier(Environment environment) {
        return environment.getProperty(BeanIdentifiers.ADAPTER_IDENTIFIER);
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    MessageTranscriber<String> httpMessageTranscriber(@Qualifier(BeanIdentifiers.OBJECT_MAPPER) @Autowired(required = false) ObjectMapper httpObjectMapper) {
        if (httpObjectMapper == null) {
            return new DefaultMessageTranscriberForStringAsTransportType();
        } else {
            return new DefaultMessageTranscriberForStringAsTransportType(httpObjectMapper);
        }
    }
}
