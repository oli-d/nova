package ch.squaredesk.nova.comm.jms.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.jms.DefaultDestinationIdGenerator;
import ch.squaredesk.nova.comm.jms.JmsAdapter;
import ch.squaredesk.nova.comm.jms.UIDCorrelationIdGenerator;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Session;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@Import({NovaProvidingConfiguration.class})
public class JmsEnablingConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(JmsEnablingConfiguration.class);

    public interface BeanIdentifiers {
        String ADAPTER_IDENTIFIER = "NOVA.JMS.ADAPTER_IDENTIFIER";
        String DEFAULT_REQUEST_TIMEOUT_IN_SECONDS = "NOVA.JMS.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS";
        String DEFAULT_MESSAGE_DELIVERY_MODE = "NOVA.JMS.DEFAULT_MESSAGE_DELIVERY_MODE";
        String DEFAULT_MESSAGE_PRIORITY = "NOVA.JMS.DEFAULT_MESSAGE_PRIORITY";
        String DEFAULT_MESSAGE_TIME_TO_LIVE = "NOVA.JMS.DEFAULT_MESSAGE_TIME_TO_LIVE";
        String CONSUMER_SESSION_ACK_MODE = "NOVA.JMS.CONSUMER_SESSION_ACK_MODE";
        String PRODUCER_SESSION_ACK_MODE = "NOVA.JMS.PRODUCER_SESSION_ACK_MODE";

        String OBJECT_MAPPER = "NOVA.JMS.OBJECT_MAPPER";
        String MESSAGE_TRANSCRIBER = "NOVA.JMS.MESSAGE_TRANSCRIBER";
        String CORRELATION_ID_GENERATOR = "NOVA.JMS.CORRELATION_ID_GENERATOR";
        String DESTINATION_ID_GENERATOR = "NOVA.JMS.DESTINATION_ID_GENERATOR";
        String CONNECTION_FACTORY = "NOVA.JMS.CONNECTION_FACTORY";
        String ADAPTER_SETTINGS = "NOVA.JMS.ADAPTER_SETTINGS";
        String AUTO_START_ADAPTER = "NOVA.JMS.AUTO_START_ADAPTER";
        String ADAPTER_STARTER = "NOVA.JMS.ADAPTER_STARTER";
        String ADAPTER = "NOVA.JMS.ADAPTER";
    }


    @Autowired
    private Environment environment;

    @Bean(BeanIdentifiers.ADAPTER)
    JmsAdapter jmsAdapter(@Qualifier(BeanIdentifiers.ADAPTER_SETTINGS) JmsAdapterSettings jmsAdapterSettings,
                          @Qualifier(BeanIdentifiers.CONNECTION_FACTORY) ConnectionFactory connectionFactory,
                          @Qualifier(BeanIdentifiers.CORRELATION_ID_GENERATOR) Supplier<String> correlationIdGenerator,
                          @Qualifier(BeanIdentifiers.DESTINATION_ID_GENERATOR) Function<Destination, String> destinationIdGenerator,
                          @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> jmsMessageTranscriber,
                          Nova nova) {
        return JmsAdapter.builder()
                .setConnectionFactory(connectionFactory)
                .setIdentifier(jmsAdapterSettings.jmsAdapterIdentifier)
                .setCorrelationIdGenerator(correlationIdGenerator)
                .setDestinationIdGenerator(destinationIdGenerator)
                .setDefaultMessageDeliveryMode(jmsAdapterSettings.defaultMessageDeliveryMode)
                .setDefaultMessagePriority(jmsAdapterSettings.defaultMessagePriority)
                .setDefaultMessageTimeToLive(jmsAdapterSettings.defaultMessageTimeToLive)
                .setDefaultRpcTimeout(jmsAdapterSettings.defaultJmsRpcTimeoutInSeconds, TimeUnit.SECONDS)
                .setConsumerSessionAckMode(jmsAdapterSettings.consumerSessionAckMode)
                .setConsumerSessionTransacted(jmsAdapterSettings.consumerSessionTransacted)
                .setProducerSessionAckMode(jmsAdapterSettings.producerSessionAckMode)
                .setProducerSessionTransacted(jmsAdapterSettings.producerSessionTransacted)
                .setMessageTranscriber(jmsMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }

    @Bean(BeanIdentifiers.ADAPTER_SETTINGS)
    JmsAdapterSettings jmsConfiguration(@Qualifier(BeanIdentifiers.ADAPTER_IDENTIFIER) @Autowired(required = false) String jmsAdapterIdentifier,
                                        @Qualifier(BeanIdentifiers.DEFAULT_MESSAGE_DELIVERY_MODE) int defaultMessageDeliveryMode,
                                        @Qualifier(BeanIdentifiers.DEFAULT_MESSAGE_PRIORITY) int defaultMessagePriority,
                                        @Qualifier(BeanIdentifiers.DEFAULT_MESSAGE_TIME_TO_LIVE) long defaultMessageTimeToLive,
                                        @Qualifier(BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS) int defaultJmsRpcTimeoutInSeconds,
                                        @Qualifier(BeanIdentifiers.CONSUMER_SESSION_ACK_MODE) int consumerSessionAckMode,
                                        @Qualifier(BeanIdentifiers.PRODUCER_SESSION_ACK_MODE) int producerSessionAckMode) {
        return JmsAdapterSettings.builder()
                .setIdentifier(jmsAdapterIdentifier)
                .setDefaultMessageDeliveryMode(defaultMessageDeliveryMode)
                .setDefaultMessagePriority(defaultMessagePriority)
                .setDefaultMessageTimeToLive(defaultMessageTimeToLive)
                .setDefaultRpcTimeoutInSeconds(defaultJmsRpcTimeoutInSeconds)
                .setConsumerSessionAckMode(consumerSessionAckMode)
                .setProducerSessionAckMode(producerSessionAckMode)
                .build();
    }

    @Bean(BeanIdentifiers.DEFAULT_MESSAGE_DELIVERY_MODE)
    int defaultMessageDeliveryMode() {
        return propertyFromEnvironmentOrConstant(BeanIdentifiers.DEFAULT_MESSAGE_DELIVERY_MODE, Message.class, Message.DEFAULT_DELIVERY_MODE, int.class);
    }

    @Bean(BeanIdentifiers.DEFAULT_MESSAGE_PRIORITY)
    int defaultMessagePriority() {
        return propertyFromEnvironmentOrConstant(BeanIdentifiers.DEFAULT_MESSAGE_PRIORITY, Message.class, Message.DEFAULT_PRIORITY, int.class);
    }

    @Bean(BeanIdentifiers.DEFAULT_MESSAGE_TIME_TO_LIVE)
    long defaultMessageTimeToLive() {
        return propertyFromEnvironmentOrConstant(BeanIdentifiers.DEFAULT_MESSAGE_TIME_TO_LIVE, Message.class, Message.DEFAULT_TIME_TO_LIVE, long.class);
    }

    @Bean(BeanIdentifiers.CONSUMER_SESSION_ACK_MODE)
    int consumerSessionAckMode() {
        return propertyFromEnvironmentOrConstant(BeanIdentifiers.CONSUMER_SESSION_ACK_MODE, Session.class, Session.AUTO_ACKNOWLEDGE, int.class);
    }

    @Bean(BeanIdentifiers.PRODUCER_SESSION_ACK_MODE)
    int producerSessionAckMode() {
        return propertyFromEnvironmentOrConstant(BeanIdentifiers.PRODUCER_SESSION_ACK_MODE, Session.class, Session.AUTO_ACKNOWLEDGE, int.class);
    }

    private <T> T propertyFromEnvironmentOrConstant (String envVariableName, Class classContainingConstant, T defaultValue, Class<T> resultType) {
        String envValueAsString = environment.getProperty(envVariableName);
        if (envValueAsString == null) {
            return defaultValue;
        }

        // try to parse env value as return type
        try {
            return environment.getProperty(envVariableName, resultType);
        } catch (Exception e) {
            logger.info("Unable to parse value \"{}\" of env parameter {} as {}. Checking, if this is a constant in class {}", envValueAsString, envVariableName, resultType, classContainingConstant);
        }

        // if the env value couldn't be parsed, it may be that the String value of a numeric constant was defined
        return constantValue(classContainingConstant, envValueAsString, resultType).orElseThrow(
                () -> new RuntimeException("Can't determine value for environment variable " + envVariableName));
    }

    private <T> Optional<T> constantValue (Class classContainingConstant, String constantName, Class<T> resultType) {
        return Arrays.stream(classContainingConstant.getDeclaredFields())
                .filter(field -> field.getName().equals(constantName))
                .filter(field -> field.getType().equals(resultType))
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .findFirst()
                .map(field -> {
                    try {
                        return (T)field.get(null);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }

    @Bean(BeanIdentifiers.DESTINATION_ID_GENERATOR)
    Function<Destination, String> jmsDestinationIdGenerator() {
        return new DefaultDestinationIdGenerator();
    }

    @Bean(BeanIdentifiers.CORRELATION_ID_GENERATOR)
    Supplier<String> jmsCorrelationIdGenerator() {
        return new UIDCorrelationIdGenerator();
    }

    @Bean(BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS)
    int defaultJmsRpcTimeoutInSeconds() {
        return environment.getProperty(BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS, Integer.class, 30);
    }

    @Bean(BeanIdentifiers.ADAPTER_IDENTIFIER)
    String jmsAdapterIdentifier() {
        return environment.getProperty(BeanIdentifiers.ADAPTER_IDENTIFIER);
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    MessageTranscriber<String> jmsMessageTranscriber(@Qualifier(BeanIdentifiers.OBJECT_MAPPER) @Autowired(required = false) ObjectMapper jmsObjectMapper) {
        if (jmsObjectMapper == null) {
            return new DefaultMessageTranscriberForStringAsTransportType();
        } else {
            return new DefaultMessageTranscriberForStringAsTransportType(jmsObjectMapper);
        }
    }

    @Bean(BeanIdentifiers.AUTO_START_ADAPTER)
    boolean autostartAdapter() {
        return environment.getProperty(BeanIdentifiers.AUTO_START_ADAPTER, boolean.class, true);
    }

    @Bean(BeanIdentifiers.ADAPTER_STARTER)
    JmsAdapterStarter jmsAdapterStarter(@Qualifier(BeanIdentifiers.ADAPTER) JmsAdapter jmsAdapter,
                                        @Qualifier(BeanIdentifiers.AUTO_START_ADAPTER) boolean autoStartAdapter) {
        return new JmsAdapterStarter(jmsAdapter, autoStartAdapter);
    }

}
