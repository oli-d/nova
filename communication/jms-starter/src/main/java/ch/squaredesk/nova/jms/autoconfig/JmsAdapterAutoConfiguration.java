/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.jms.autoconfig;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.jms.DefaultDestinationIdGenerator;
import ch.squaredesk.nova.comm.jms.JmsAdapter;
import ch.squaredesk.nova.comm.jms.UIDCorrelationIdGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@ImportAutoConfiguration(JmsAdapterMessageTranscriberAutoConfiguration.class)
@EnableConfigurationProperties(JmsAdapterAutoConfigurationProperties.class)
@ConditionalOnBean({ConnectionFactory.class, Nova.class})
public class JmsAdapterAutoConfiguration {
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
                .setDefaultRpcTimeout(jmsAdapterAutoConfigurationProperties.getDefaultJmsRpcTimeoutInSeconds(), TimeUnit.SECONDS)
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
