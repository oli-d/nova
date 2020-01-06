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

package ch.squaredesk.nova.comm.http.autoconfig;

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpAdapterMessageTranscriberAutoConfig {
    @Bean(BeanIdentifiers.OBJECT_MAPPER)
    @ConditionalOnMissingBean(name = BeanIdentifiers.OBJECT_MAPPER)
    @ConditionalOnClass(ObjectMapper.class)
    ObjectMapper httpObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules()
                ;
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnMissingBean(MessageTranscriber.class)
    @ConditionalOnBean(name = BeanIdentifiers.OBJECT_MAPPER)
    MessageTranscriber<String> httpMessageTranscriberWithJackson(@Qualifier(BeanIdentifiers.OBJECT_MAPPER) ObjectMapper jmsObjectMapper) {
        return new DefaultMessageTranscriberForStringAsTransportType(jmsObjectMapper);
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnMissingBean(name = { BeanIdentifiers.MESSAGE_TRANSCRIBER, BeanIdentifiers.OBJECT_MAPPER })
    MessageTranscriber<String> httpMessageTranscriberWithoutJackson() {
        return new DefaultMessageTranscriberForStringAsTransportType();
    }

}
