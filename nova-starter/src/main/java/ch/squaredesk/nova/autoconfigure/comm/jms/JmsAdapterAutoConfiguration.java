/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.jms;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfigure.core.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.jms.DefaultDestinationIdGenerator;
import ch.squaredesk.nova.comm.jms.JmsAdapter;
import ch.squaredesk.nova.comm.jms.UIDCorrelationIdGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@AutoConfigureAfter(NovaAutoConfiguration.class)
@EnableConfigurationProperties(JmsAdapterAutoConfigurationProperties.class)
@ConditionalOnBean({ConnectionFactory.class})
@ConditionalOnClass(JmsAdapter.class)
public class JmsAdapterAutoConfiguration {
    @Bean(BeanIdentifiers.OBJECT_MAPPER)
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean(name = BeanIdentifiers.OBJECT_MAPPER)
    ObjectMapper jmsObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules()
                ;
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnMissingBean(name = BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnBean(name = BeanIdentifiers.OBJECT_MAPPER)
    MessageTranscriber<String> jmsMessageTranscriberWithObjectMapper(@Qualifier(BeanIdentifiers.OBJECT_MAPPER) ObjectMapper jmsObjectMapper) {
        return new DefaultMessageTranscriberForStringAsTransportType(jmsObjectMapper);
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnMissingBean(name = {BeanIdentifiers.MESSAGE_TRANSCRIBER, BeanIdentifiers.OBJECT_MAPPER})
    MessageTranscriber<String> jmsMessageTranscriberWithoutObjectMapper() {
        return new DefaultMessageTranscriberForStringAsTransportType();
    }

    @Bean
    @ConditionalOnMissingBean(JmsAdapter.class)
    JmsAdapter jmsAdapter(JmsAdapterAutoConfigurationProperties jmsAdapterAutoConfigurationProperties,
                          ConnectionFactory connectionFactory,
                          @Qualifier(BeanIdentifiers.CORRELATION_ID_GENERATOR)
                          Supplier<String> correlationIdGenerator,
                          @Qualifier(BeanIdentifiers.DESTINATION_ID_GENERATOR)
                          Function<Destination, String> destinationIdGenerator,
                          @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER)
                          MessageTranscriber<String> jmsMessageTranscriber,
                          Nova nova) {
        return JmsAdapter.builder()
                .setIdentifier(jmsAdapterAutoConfigurationProperties.getAdapterIdentifier())
                .setDefaultMessageDeliveryMode(jmsAdapterAutoConfigurationProperties.getDefaultMessageDeliveryMode())
                .setDefaultMessagePriority(jmsAdapterAutoConfigurationProperties.getDefaultMessagePriority())
                .setDefaultMessageTimeToLive(jmsAdapterAutoConfigurationProperties.getDefaultMessageTimeToLive())
                .setDefaultRpcTimeout(Duration.ofSeconds(jmsAdapterAutoConfigurationProperties.getDefaultJmsRpcTimeoutInSeconds()))
                .setConsumerSessionAckMode(jmsAdapterAutoConfigurationProperties.getConsumerSessionAckMode())
                .setConsumerSessionTransacted(jmsAdapterAutoConfigurationProperties.isConsumerSessionTransacted())
                .setProducerSessionAckMode(jmsAdapterAutoConfigurationProperties.getProducerSessionAckMode())
                .setProducerSessionTransacted(jmsAdapterAutoConfigurationProperties.isProducerSessionTransacted())
                .setConnectionFactory(connectionFactory)
                .setCorrelationIdGenerator(correlationIdGenerator)
                .setDestinationIdGenerator(destinationIdGenerator)
                .setMessageTranscriber(jmsMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }

    @Bean(BeanIdentifiers.DESTINATION_ID_GENERATOR)
    @ConditionalOnMissingBean(name = BeanIdentifiers.DESTINATION_ID_GENERATOR)
    Function<Destination, String> jmsDestinationIdGenerator() {
        return new DefaultDestinationIdGenerator();
    }

    @Bean(BeanIdentifiers.CORRELATION_ID_GENERATOR)
    @ConditionalOnMissingBean(name = BeanIdentifiers.CORRELATION_ID_GENERATOR)
    Supplier<String> jmsCorrelationIdGenerator() {
        return new UIDCorrelationIdGenerator();
    }

    @Bean
    @ConditionalOnBean(JmsAdapter.class)
    @ConditionalOnProperty(name = "nova.jms.auto-start-adapter", havingValue = "true", matchIfMissing = true)
    JmsAdapterStarter jmsAdapterStarter(JmsAdapter jmsAdapter) {
        return new JmsAdapterStarter(jmsAdapter);
    }

}
