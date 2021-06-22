/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.kafka;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfigure.core.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.kafka.KafkaAdapter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(KafkaAdapterConfigurationProperties.class)
@AutoConfigureAfter(NovaAutoConfiguration.class)
@ConditionalOnClass(KafkaAdapter.class)
public class KafkaAdapterAutoConfiguration {

    @Bean(BeanIdentifiers.OBJECT_MAPPER)
    @ConditionalOnMissingBean(name = BeanIdentifiers.OBJECT_MAPPER)
    @ConditionalOnClass(ObjectMapper.class)
    ObjectMapper kafkaObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules()
                ;
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnMissingBean(name = BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnBean(name = BeanIdentifiers.OBJECT_MAPPER)
    MessageTranscriber<String> kafkaMessageTranscriberWithMapper(@Qualifier(BeanIdentifiers.OBJECT_MAPPER) ObjectMapper jmsObjectMapper) {
        return new DefaultMessageTranscriberForStringAsTransportType(jmsObjectMapper);
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnMissingBean(name = {BeanIdentifiers.MESSAGE_TRANSCRIBER, BeanIdentifiers.OBJECT_MAPPER})
    MessageTranscriber<String> kafkaMessageTranscriberWithoutMapper() {
        return new DefaultMessageTranscriberForStringAsTransportType();
    }

    @Bean
    @ConditionalOnMissingBean(KafkaAdapter.class)
    KafkaAdapter kafkaAdapter(KafkaAdapterConfigurationProperties kafkaAdapterConfigurationProperties,
                              @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> kafkaMessageTranscriber,
                              Nova nova) {
        return KafkaAdapter.builder()
                .setServerAddress(kafkaAdapterConfigurationProperties.getServerAddress())
                .setBrokerClientId(kafkaAdapterConfigurationProperties.getBrokerClientId())
                .setConsumerProperties(kafkaAdapterConfigurationProperties.getConsumerProperties())
                .setConsumerGroupId(kafkaAdapterConfigurationProperties.getConsumerGroupId())
                .setMessagePollingTimeout(Duration.ofMillis(kafkaAdapterConfigurationProperties.getPollTimeoutInMs()))
                .setProducerProperties(kafkaAdapterConfigurationProperties.getProducerProperties())
                .setIdentifier(kafkaAdapterConfigurationProperties.getAdapterIdentifier())
                .setMessageTranscriber(kafkaMessageTranscriber)
                .build();
    }

    @Bean
    @ConditionalOnBean
    KafkaAdapterStarter kafkaAdapterStarter(KafkaAdapter kafkaAdapter) {
        return new KafkaAdapterStarter(kafkaAdapter);
    }
}
