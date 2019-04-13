package ch.squaredesk.nova.comm.kafka.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.kafka.KafkaAdapter;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Configuration
@Import(NovaProvidingConfiguration.class)
public class KafkaEnablingConfiguration {
    public interface BeanIdentifiers {
        String ADAPTER_IDENTIFIER = "NOVA.KAFKA.ADAPTER_IDENTIFIER";
        String POLL_TIMEOUT_IN_MS = "NOVA.KAFKA.POLL_TIMEOUT_IN_MILLISECONDS";
        String SERVER_ADDRESS = "NOVA.KAFKA.SERVER_ADDRESS";

        String OBJECT_MAPPER = "NOVA.KAFKA.OBJECT_MAPPER";
        String MESSAGE_TRANSCRIBER = "NOVA.KAFKA.MESSAGE_TRANSCRIBER";
        String CONSUMER_PROPERTIES = "NOVA.KAFKA.CONSUMER_PROPERTIES";
        String PRODUCER_PROPERTIES = "NOVA.KAFKA.PRODUCER_PROPERTIES";
        String ADAPTER_SETTINGS = "NOVA.KAFKA.ADAPTER_SETTINGS";
        String ADAPTER = "NOVA.KAFKA.ADAPTER";
    }

    @Bean(BeanIdentifiers.ADAPTER)
    KafkaAdapter kafkaAdapter (@Qualifier(BeanIdentifiers.ADAPTER_SETTINGS) KafkaAdapterSettings kafkaAdapterSettings,
                               @Qualifier(BeanIdentifiers.CONSUMER_PROPERTIES) @Autowired(required = false) Properties consumerProperties,
                               @Qualifier(BeanIdentifiers.PRODUCER_PROPERTIES) @Autowired(required = false) Properties producerProperties,
                               @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> kafkaMessageTranscriber,
                               Nova nova) {
        return KafkaAdapter.builder()
                .setServerAddress(kafkaAdapterSettings.serverAddress)
                .setConsumerProperties(consumerProperties)
                .setMessagePollingTimeout(kafkaAdapterSettings.pollTimeoutInMilliseconds, TimeUnit.MILLISECONDS)
                .setProducerProperties(producerProperties)
                .setIdentifier(kafkaAdapterSettings.identifier)
                .setMessageTranscriber(kafkaMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }

    @Bean(BeanIdentifiers.ADAPTER_IDENTIFIER)
    String kafkaAdapterIdentifier (Environment environment) {
        return environment.getProperty(BeanIdentifiers.ADAPTER_IDENTIFIER);
    }

    @Bean(BeanIdentifiers.SERVER_ADDRESS)
    String kafkaServerAddress (Environment environment) {
        return environment.getProperty(BeanIdentifiers.SERVER_ADDRESS);
    }

    @Bean(BeanIdentifiers.POLL_TIMEOUT_IN_MS)
    long kafkaPollTimeoutInMilliseconds (Environment environment) {
        return environment.getProperty(BeanIdentifiers.POLL_TIMEOUT_IN_MS, long.class, 1000L);
    }

    @Bean(BeanIdentifiers.ADAPTER_SETTINGS)
    KafkaAdapterSettings kafkaAdapterSettings (@Qualifier(BeanIdentifiers.ADAPTER_IDENTIFIER) String kafkaAdapterIdentifier,
                               @Qualifier(BeanIdentifiers.SERVER_ADDRESS) String kafkaServerAddress,
                               @Qualifier(BeanIdentifiers.POLL_TIMEOUT_IN_MS) long kafkaPollTimeoutInMilliseconds) {
        return new KafkaAdapterSettings(
            kafkaAdapterIdentifier,
            kafkaServerAddress,
            kafkaPollTimeoutInMilliseconds
        );
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    MessageTranscriber<String> kafkaMessageTranscriber(@Qualifier(BeanIdentifiers.OBJECT_MAPPER) @Autowired(required = false) ObjectMapper kafkaObjectMapper) {
        if (kafkaObjectMapper == null) {
            return new DefaultMessageTranscriberForStringAsTransportType();
        } else {
            return new DefaultMessageTranscriberForStringAsTransportType(kafkaObjectMapper);
        }
    }
}
