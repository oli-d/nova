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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

@Configuration
@Import(KafkaAdapterMessageTranscriberAutoConfiguration.class)
@EnableConfigurationProperties(KafkaAdapterSettings.class)
public class KafkaAdapterAutoConfig {

    @Bean
    @ConditionalOnMissingBean(KafkaAdapter.class)
    KafkaAdapter kafkaAdapter(KafkaAdapterSettings kafkaAdapterSettings,
                              @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> kafkaMessageTranscriber,
                              Nova nova) {
        return KafkaAdapter.builder()
                .setServerAddress(kafkaAdapterSettings.getServerAddress())
                .setBrokerClientId(kafkaAdapterSettings.getBrokerClientId())
                .setConsumerProperties(kafkaAdapterSettings.getConsumerProperties())
                .setConsumerGroupId(kafkaAdapterSettings.getConsumerGroupId())
                .setMessagePollingTimeout(kafkaAdapterSettings.getPollTimeoutInMs(), TimeUnit.MILLISECONDS)
                .setProducerProperties(kafkaAdapterSettings.getProducerProperties())
                .setIdentifier(kafkaAdapterSettings.getAdapterIdentifier())
                .setMessageTranscriber(kafkaMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }
}
