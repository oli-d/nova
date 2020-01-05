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

package ch.squaredesk.nova.comm.kafka.autoconfig;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.kafka.KafkaAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

@Configuration
@Import(KafkaAdapterMessageTranscriberAutoConfiguration.class)
public class KafkaAdapterAutoConfig {

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
                .setMessagePollingTimeout(kafkaAdapterConfigurationProperties.getPollTimeoutInMs(), TimeUnit.MILLISECONDS)
                .setProducerProperties(kafkaAdapterConfigurationProperties.getProducerProperties())
                .setIdentifier(kafkaAdapterConfigurationProperties.getAdapterIdentifier())
                .setMessageTranscriber(kafkaMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }
}
