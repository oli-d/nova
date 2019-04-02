package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.HttpAdapter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.concurrent.TimeUnit;

@Configuration
@Import(HttpServerProvidingConfiguration.class)
public class HttpEnablingConfiguration {
    @Autowired
    private Environment environment;

    @Autowired
    @Qualifier("httpServer")
    private HttpServer httpServer;

    @Bean("httpAdapter")
    HttpAdapter httpAdapter() {
        return HttpAdapter.builder()
                .setDefaultRequestTimeout(defaultHttpRequestTimeoutInSeconds(), TimeUnit.SECONDS)
                .setHttpServer(httpServer)
                .setIdentifier(defaultHttpAdapterIdentifier())
                .setMessageTranscriber(httpMessageTranscriber())
                .build();
    }

    @Bean("defaultHttpRequestTimeoutInSeconds")
    public int defaultHttpRequestTimeoutInSeconds() {
        return environment.getProperty("NOVA.HTTP.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS", Integer.class, 30);
    }

    @Bean("defaultHttpAdapterIdentifier")
    public String defaultHttpAdapterIdentifier() {
        return environment.getProperty("NOVA.HTTP.ADAPTER_IDENTIFIER");
    }

    @Bean("httpMessageTranscriber")
    public MessageTranscriber<String> httpMessageTranscriber() {
        return new DefaultMessageTranscriberForStringAsTransportType();
    }
}
