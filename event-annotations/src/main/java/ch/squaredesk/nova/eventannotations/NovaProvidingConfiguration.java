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
import io.reactivex.BackpressureStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class NovaProvidingConfiguration {
    @Autowired
    private Environment environment;

    @Bean(name = "nova")
    public Nova getNova() {
        return Nova.builder()
                .setIdentifier(getIdentifier())
                .setDefaultBackpressureStrategy(getDefaultBackpressureStrategy())
                .setWarnOnUnhandledEvent(getWarnOnUnhandledEvent())
                .build();
    }

    @Bean
    public String getIdentifier() {
        return environment.getProperty("NOVA.ID", "");
    }

    @Bean
    public Boolean getWarnOnUnhandledEvent() {
        return environment.getProperty("NOVA.EVENTS.WARN_ON_UNHANDLED", Boolean.class, false);
    }

    @Bean
    public BackpressureStrategy getDefaultBackpressureStrategy() {
        String strategyAsString = environment.getProperty(
                "NOVA.EVENTS.BACKPRESSURE_STRATEGY",
                String.class,
                BackpressureStrategy.BUFFER.toString());
        return BackpressureStrategy.valueOf(strategyAsString.toUpperCase());
    }

}
