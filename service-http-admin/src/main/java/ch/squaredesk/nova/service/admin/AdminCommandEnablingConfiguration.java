/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service.admin;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpCommAdapter;
import ch.squaredesk.nova.service.admin.messages.AdminMessage;
import ch.squaredesk.nova.service.admin.messages.Error;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public abstract class AdminCommandEnablingConfiguration {
    @Autowired
    Environment environment;

    @Autowired
    Nova nova;

    @Bean
    public AdminCommandBeanPostprocessor getBeanPostProcessor() {
        return new AdminCommandBeanPostprocessor(adminInfoCenter());
    }

    @Bean
    public Integer adminPort() {
        return environment.getProperty("NOVA.ADMIN.PORT", Integer.class, 8888);
    }

    @Bean
    public String adminBaseUrl() {
        return environment.getProperty("NOVA.ADMIN.BASE_URL", "/admin");
    }

    @Bean
    public String adminInterfaceName() {
        String interfaceName = environment.getProperty("NOVA.ADMIN.INTERFACE_NAME", "");
        if ("".equals(interfaceName)) {
            interfaceName = "localhost";
            // FIXME
        }
        return interfaceName;
    }

    @Bean
    public AdminInfoCenter adminInfoCenter() {
        return new AdminInfoCenter(adminUrlCalculator());
    }

    @Bean
    public AdminUrlCalculator adminUrlCalculator() {
        return new AdminUrlCalculator(adminInterfaceName(), adminBaseUrl(), adminPort());
    }


    @Bean
    public ObjectMapper adminObjectMapper() {
        return new ObjectMapper();
    }

    @Bean(name = "adminCommAdapter")
    public HttpCommAdapter<AdminMessage> adminCommAdapter() {
        ObjectMapper om = adminObjectMapper();

        return HttpCommAdapter.<AdminMessage>builder()
                .setMessageMarshaller(adminMessage -> om.writeValueAsString(adminMessage))
                .setMessageUnmarshaller(incomingMessage -> om.readValue(incomingMessage, AdminMessage.class))
                .setErrorReplyFactory(error -> new Error("1", error.getMessage()))
                .setIdentifier("AdminCommAdapter")
                .setMetrics(nova.metrics)
                .setServerPort(adminPort())
                .build();
    }

    @Bean
    public AdminCommandServer adminRequestServer() {
        return new AdminCommandServer(adminInfoCenter());
    }
}
