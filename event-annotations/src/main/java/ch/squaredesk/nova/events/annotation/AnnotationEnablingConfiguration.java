/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events.annotation;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.Timer;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

@Configuration
public class AnnotationEnablingConfiguration {
    @Autowired
    protected Nova nova;

    @Bean
    public static  EventHandlingBeanPostprocessor eventHandlingBeanPostProcessor() {
        return new EventHandlingBeanPostprocessor();
    }

}

