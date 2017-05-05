/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.eventannotations;

import ch.squaredesk.nova.Nova;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class AnnotationEnablingConfiguration {
    @Autowired
    protected Nova nova;

    @Bean
    public EventHandlingBeanPostprocessor getBeanPostProcessor() {
        return new EventHandlingBeanPostprocessor(nova.identifier, nova.eventBus, nova.metrics);
    }
}