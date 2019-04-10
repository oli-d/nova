package ch.squaredesk.nova.comm.websockets.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.spring.HttpServerProvidingConfiguration;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
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
    @Autowired
    Environment environment;

    @Bean("webSocketAdapter")
    WebSocketAdapter webSocketAdapter(@Qualifier("webSocketAdapterIdentifier") @Autowired(required = false) String webSocketAdapterIdentifier,
                                      @Qualifier("webSocketMessageTranscriber") MessageTranscriber<String> webSocketMessageTranscriber,
                                      HttpServer httpServer,
                                      @Qualifier("nova") Nova nova) {
        return WebSocketAdapter.builder()
                .setIdentifier(webSocketAdapterIdentifier)
                .setHttpServer(httpServer)
                .setMessageTranscriber(webSocketMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }

    @Bean("webSocketAdapterIdentifier")
    String webSocketAdapterIdentifier() {
        return environment.getProperty("NOVA.WEB_SOCKET.ADAPTER_IDENTIFIER");
    }

    @Bean("webSocketMessageTranscriber")
    MessageTranscriber<String> webSocketMessageTranscriber(@Qualifier("webSocketObjectMapper") @Autowired(required = false)ObjectMapper webSocketObjectMapper) {
        if (webSocketObjectMapper == null) {
            return new DefaultMessageTranscriberForStringAsTransportType();
        } else {
            return new DefaultMessageTranscriberForStringAsTransportType(webSocketObjectMapper);
        }
    }

    @Bean("webSocketBeanPostprocessor")
    WebSocketBeanPostprocessor webSocketBeanPostprocessor(
            @Qualifier("webSocketMessageTranscriber") MessageTranscriber<String> webSocketMessageTranscriber,
            @Qualifier("webSocketAdapterIdentifier") @Autowired(required = false) String webSocketAdapterIdentifier,
            Nova nova) {
        return new WebSocketBeanPostprocessor(webSocketMessageTranscriber, new MetricsCollector(webSocketAdapterIdentifier, nova.metrics));
    }
}
