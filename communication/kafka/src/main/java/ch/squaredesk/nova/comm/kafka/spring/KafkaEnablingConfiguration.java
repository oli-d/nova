package ch.squaredesk.nova.comm.kafka.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.kafka.KafkaAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Configuration
public class KafkaEnablingConfiguration {

    @Autowired
    private Environment environment;

    @Bean("kafkaAdapter")
    KafkaAdapter kafkaAdapter (@Qualifier("kafkaAdapterSettings") KafkaAdapterSettings kafkaAdapterSettings,
                               @Qualifier("kafkaConsumerProperties") @Autowired(required = false) Properties consumerProperties,
                               @Qualifier("kafkaProducerProperties") @Autowired(required = false) Properties producerProperties,
                               @Qualifier("kafkaMessageTranscriber") MessageTranscriber<String> kafkaMessageTranscriber,
                               @Qualifier("nova") Nova nova) {
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

    @Bean("kafkaAdapterIdentifier")
    String kafkaAdapterIdentifier () {
        return environment.getProperty("NOVA.KAFKA.ADAPTER_IDENTIFIER");
    }

    @Bean("kafkaServerAddress")
    String kafkaServerAddress () {
        return environment.getProperty("NOVA.KAFKA.SERVER_ADDRESS");
    }

    @Bean("kafkaPollTimeoutInMilliseconds")
    long kafkaPollTimeoutInMilliseconds () {
        return environment.getProperty("NOVA.KAFKA.POLL_TIMEOUT_IN_MILLISECONDS", long.class, 1000L);
    }

    @Bean("kafkaAdapterSettings")
    KafkaAdapterSettings kafkaAdapterSettings (@Qualifier("kafkaAdapterIdentifier") String kafkaAdapterIdentifier,
                               @Qualifier("kafkaServerAddress") String kafkaServerAddress,
                               @Qualifier("kafkaPollTimeoutInMilliseconds") long kafkaPollTimeoutInMilliseconds) {
        return new KafkaAdapterSettings(
            kafkaAdapterIdentifier,
            kafkaServerAddress,
            kafkaPollTimeoutInMilliseconds
        );
    }

    @Bean("kafkaMessageTranscriber")
    MessageTranscriber<String> kafkaMessageTranscriber(@Qualifier("kafkaObjectMapper") @Autowired(required = false) ObjectMapper kafkaObjectMapper) {
        if (kafkaObjectMapper == null) {
            return new DefaultMessageTranscriberForStringAsTransportType();
        } else {
            return new DefaultMessageTranscriberForStringAsTransportType(kafkaObjectMapper);
        }
    }
}
