package ch.squaredesk.nova.comm.jms.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.jms.DefaultDestinationIdGenerator;
import ch.squaredesk.nova.comm.jms.JmsAdapter;
import ch.squaredesk.nova.comm.jms.UIDCorrelationIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class JmsEnablingConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(JmsEnablingConfiguration.class);

    @Autowired
    private Environment environment;

    @Bean("jmsAdapter")
    JmsAdapter jmsAdapter(@Qualifier("jmsAdapterSettings") JmsAdapterSettings jmsAdapterSettings,
                          @Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory,
                          @Qualifier("jmsCorrelationIdGenerator") Supplier<String> correlationIdGenerator,
                          @Qualifier("jmsDestinationIdGenerator") Function<Destination, String> destinationIdGenerator,
                          @Qualifier("jmsMessageTranscriber") MessageTranscriber<String> jmsMessageTranscriber,
                          @Qualifier("nova") Nova nova) {
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

    @Bean("jmsAdapterSettings")
    JmsAdapterSettings jmsConfiguration(@Qualifier("jmsAdapterIdentifier") @Autowired(required = false) String jmsAdapterIdentifier,
                                        @Qualifier("defaultMessageDeliveryMode") int defaultMessageDeliveryMode,
                                        @Qualifier("defaultMessagePriority") int defaultMessagePriority,
                                        @Qualifier("defaultMessageTimeToLive") long defaultMessageTimeToLive,
                                        @Qualifier("defaultJmsRpcTimeoutInSeconds") int defaultJmsRpcTimeoutInSeconds,
                                        @Qualifier("consumerSessionAckMode") int consumerSessionAckMode,
                                        @Qualifier("producerSessionAckMode") int producerSessionAckMode) {
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

    @Bean("defaultMessageDeliveryMode")
    int defaultMessageDeliveryMode() {
        return propertyFromEnvironmentOrConstant("NOVA.JMS.DEFAULT_MESSAGE_DELIVERY_MODE", Message.class, Message.DEFAULT_DELIVERY_MODE, int.class);
    }

    @Bean("defaultMessagePriority")
    int defaultMessagePriority() {
        return propertyFromEnvironmentOrConstant("NOVA.JMS.DEFAULT_MESSAGE_PRIORITY", Message.class, Message.DEFAULT_PRIORITY, int.class);
    }

    @Bean("defaultMessageTimeToLive")
    long defaultMessageTimeToLive() {
        return propertyFromEnvironmentOrConstant("NOVA.JMS.DEFAULT_MESSAGE_TIME_TO_LIVE", Message.class, Message.DEFAULT_TIME_TO_LIVE, long.class);
    }

    @Bean("consumerSessionAckMode")
    int consumerSessionAckMode() {
        return propertyFromEnvironmentOrConstant("NOVA.JMS.CONSUMER_SESSION_ACK_MODE", Session.class, Session.AUTO_ACKNOWLEDGE, int.class);
    }

    @Bean("producerSessionAckMode")
    int producerSessionAckMode() {
        return propertyFromEnvironmentOrConstant("NOVA.JMS.PRODUCER_SESSION_ACK_MODE", Session.class, Session.AUTO_ACKNOWLEDGE, int.class);
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

    @Bean("jmsDestinationIdGenerator")
    Function<Destination, String> jmsDestinationIdGenerator() {
        return new DefaultDestinationIdGenerator();
    }

    @Bean("jmsCorrelationIdGenerator")
    Supplier<String> jmsCorrelationIdGenerator() {
        return new UIDCorrelationIdGenerator();
    }

    @Bean("defaultJmsRpcTimeoutInSeconds")
    int defaultJmsRpcTimeoutInSeconds() {
        return environment.getProperty("NOVA.JMS.DEFAULT_RPC_TIMEOUT_IN_SECONDS", Integer.class, 30);
    }

    @Bean("jmsAdapterIdentifier")
    String jmsAdapterIdentifier() {
        return environment.getProperty("NOVA.JMS.ADAPTER_IDENTIFIER");
    }

    @Bean("jmsMessageTranscriber")
    MessageTranscriber<String> jmsMessageTranscriber() {
        return new DefaultMessageTranscriberForStringAsTransportType();
    }
}
