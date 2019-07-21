package ch.squaredesk.nova.comm.websockets.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.spring.HttpServerProvidingConfiguration;
import ch.squaredesk.nova.comm.websockets.WebSocketAdapter;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@Import({HttpServerProvidingConfiguration.class, NovaProvidingConfiguration.class})
public class WebSocketEnablingConfiguration {
    public interface BeanIdentifiers {
        String CAPTURE_METRICS = "NOVA.WEB_SOCKET.CAPTURE_METRICS";
        String ADAPTER_IDENTIFIER = "NOVA.WEB_SOCKET.ADAPTER_IDENTIFIER";

        String OBJECT_MAPPER = "NOVA.WEB_SOCKET.OBJECT_MAPPER";
        String MESSAGE_TRANSCRIBER = "NOVA.WEB_SOCKET.MESSAGE_TRANSCRIBER";
        String ADAPTER = "NOVA.WEB_SOCKET.ADAPTER.INSTANCE";
    }

    @Bean(BeanIdentifiers.ADAPTER)
    WebSocketAdapter webSocketAdapter(@Qualifier(BeanIdentifiers.ADAPTER_IDENTIFIER) @Autowired(required = false) String webSocketAdapterIdentifier,
                                      @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> webSocketMessageTranscriber,
                                      @Qualifier(HttpServerProvidingConfiguration.BeanIdentifiers.SERVER) @Autowired(required = false) HttpServer httpServer,
                                      Nova nova) {
        return WebSocketAdapter.builder()
                .setIdentifier(webSocketAdapterIdentifier)
                .setHttpServer(httpServer)
                .setMessageTranscriber(webSocketMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }

    @Bean(BeanIdentifiers.ADAPTER_IDENTIFIER)
    String webSocketAdapterIdentifier(Environment environment) {
        return environment.getProperty(BeanIdentifiers.ADAPTER_IDENTIFIER);
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    MessageTranscriber<String> webSocketMessageTranscriber(@Qualifier(BeanIdentifiers.OBJECT_MAPPER) @Autowired(required = false)ObjectMapper webSocketObjectMapper) {
        if (webSocketObjectMapper == null) {
            return new DefaultMessageTranscriberForStringAsTransportType();
        } else {
            return new DefaultMessageTranscriberForStringAsTransportType(webSocketObjectMapper);
        }
    }

    @Bean
    WebSocketBeanProcessor webSocketBeanPostprocessor(
            @Qualifier(BeanIdentifiers.ADAPTER_IDENTIFIER) @Autowired(required = false) String webSocketAdapterIdentifier,
            @Qualifier(BeanIdentifiers.ADAPTER) WebSocketAdapter webSocketAdapter,
            @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> webSocketMessageTranscriber,
            Nova nova) {
        return new WebSocketBeanProcessor(webSocketAdapter, webSocketMessageTranscriber, webSocketAdapterIdentifier, nova.metrics);
    }
}
