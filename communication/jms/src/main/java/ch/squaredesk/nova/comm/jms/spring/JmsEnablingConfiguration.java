package ch.squaredesk.nova.comm.jms.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.jms.DefaultDestinationIdGenerator;
import ch.squaredesk.nova.comm.jms.JmsAdapter;
import ch.squaredesk.nova.comm.jms.UIDCorrelationIdGenerator;
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
    @Autowired
    private Environment environment;

    @Bean("jmsAdapter")
    JmsAdapter jmsAdapter(@Qualifier("jmsConfiguration") JmsConfiguration jmsConfiguration,
                          @Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory,
                          @Qualifier("jmsCorrelationIdGenerator") Supplier<String> correlationIdGenerator,
                          @Qualifier("jmsDestinationIdGenerator") Function<Destination, String> destinationIdGenerator,
                          @Qualifier("jmsMessageTranscriber") MessageTranscriber<String> jmsMessageTranscriber,
                          Nova nova) {
        return JmsAdapter.builder()
                .setConnectionFactory(connectionFactory)
                .setIdentifier(jmsConfiguration.jmsAdapterIdentifier)
                .setCorrelationIdGenerator(correlationIdGenerator)
                .setDestinationIdGenerator(destinationIdGenerator)
                .setDefaultMessageDeliveryMode(jmsConfiguration.defaultMessageDeliveryMode)
                .setDefaultMessagePriority(jmsConfiguration.defaultMessagePriority)
                .setDefaultMessageTimeToLive(jmsConfiguration.defaultMessageTimeToLive)
                .setDefaultRpcTimeout(jmsConfiguration.defaultJmsRpcTimeoutInSeconds, TimeUnit.SECONDS)
                .setConsumerSessionAckMode(jmsConfiguration.consumerSessionAckMode)
                .setConsumerSessionTransacted(jmsConfiguration.consumerSessionTransacted)
                .setProducerSessionAckMode(jmsConfiguration.producerSessionAckMode)
                .setProducerSessionTransacted(jmsConfiguration.producerSessionTransacted)
                .setMessageTranscriber(jmsMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }

    @Bean("jmsConfiguration")
    JmsConfiguration jmsConfiguration(@Qualifier("jmsAdapterIdentifier") String jmsAdapterIdentifier,
                          @Qualifier("defaultMessageDeliveryMode") int defaultMessageDeliveryMode,
                          @Qualifier("defaultMessagePriority") int defaultMessagePriority,
                          @Qualifier("defaultMessageTimeToLive") long defaultMessageTimeToLive,
                          @Qualifier("defaultJmsRpcTimeoutInSeconds") int defaultJmsRpcTimeoutInSeconds,
                          @Qualifier("consumerSessionAckMode") int consumerSessionAckMode,
                          @Qualifier("consumerSessionTransacted") boolean consumerSessionTransacted,
                          @Qualifier("producerSessionAckMode") int producerSessionAckMode,
                          @Qualifier("producerSessionTransacted") boolean producerSessionTransacted) {
        return JmsConfiguration.builder()
                .setIdentifier(jmsAdapterIdentifier)
                .setDefaultMessageDeliveryMode(defaultMessageDeliveryMode)
                .setDefaultMessagePriority(defaultMessagePriority)
                .setDefaultMessageTimeToLive(defaultMessageTimeToLive)
                .setDefaultRpcTimeoutInSeconds(defaultJmsRpcTimeoutInSeconds)
                .setConsumerSessionAckMode(consumerSessionAckMode)
                .setConsumerSessionTransacted(consumerSessionTransacted)
                .setProducerSessionAckMode(producerSessionAckMode)
                .setProducerSessionTransacted(producerSessionTransacted)
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

    @Bean("consumerSessionTransacted")
    boolean consumerSessionTransacted() {
        return environment.getProperty("NOVA.JMS.CONSUMER_SESSION_TRANSACTED", Boolean.class, false);
    }

    @Bean("producerSessionAckMode")
    int producerSessionAckMode() {
        return propertyFromEnvironmentOrConstant("NOVA.JMS.PRODUCER_SESSION_ACK_MODE", Session.class, Session.AUTO_ACKNOWLEDGE, int.class);
    }

    @Bean("producerSessionTransacted")
    boolean producerSessionTransacted() {
        return environment.getProperty("NOVA.JMS.PRODUCER_SESSION_TRANSACTED", Boolean.class, false);
    }

    private <T> T propertyFromEnvironmentOrConstant (String envVariableName, Class classContainingConstant, T defaultValue, Class<T> resultType) {
        String envVariableValueAsString = environment.getProperty(envVariableName, String.valueOf(defaultValue));

        // try to parse env value as int
        Optional<T> envVariableValue = parse(envVariableValueAsString, resultType);

        if (envVariableValue.isPresent()) {
            return envVariableValue.get();
        } else {
            return constantValue(classContainingConstant, envVariableValueAsString, resultType)
                    .orElseThrow(() -> new RuntimeException("Unable to parse value \"" + envVariableValueAsString + "\" for env parameter " + envVariableName + " as " + resultType));
        }
    }

    private <T> Optional<T> parse (String value, Class<T> resultType) {
        T returnValue = null;
        try {
            returnValue = (T)Integer.parseInt(value);
        } catch (Exception e) {
            // noop
        }
        return Optional.ofNullable(returnValue);
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
    public Function<Destination, String> jmsDestinationIdGenerator() {
        return new DefaultDestinationIdGenerator();
    }

    @Bean("jmsCorrelationIdGenerator")
    public Supplier<String> jmsCorrelationIdGenerator() {
        return new UIDCorrelationIdGenerator();
    }

    @Bean("defaultJmsRpcTimeoutInSeconds")
    public int defaultJmsRpcTimeoutInSeconds() {
        return environment.getProperty("NOVA.JMS.DEFAULT_RPC_TIMEOUT_IN_SECONDS", Integer.class, 30);
    }

    @Bean("jmsAdapterIdentifier")
    public String jmsAdapterIdentifier() {
        return environment.getProperty("NOVA.JMS.ADAPTER_IDENTIFIER");
    }

    @Bean("jmsMessageTranscriber")
    public MessageTranscriber<String> jmsMessageTranscriber() {
        return new DefaultMessageTranscriberForStringAsTransportType();
    }
}
