/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http.annotation;

import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class SpringWiringTest {

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    private String serverUrl;

    private void setupContext(Class configClass) throws Exception {
        ctx.register(configClass);
        ctx.refresh();
        HttpServerConfiguration cfg = ctx.getBean(HttpServerConfiguration.class);
        serverUrl = "http://" + cfg.interfaceName + ":" + cfg.port;
        ctx.getBean(HttpServer.class).start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        ctx.getBean(HttpServer.class).shutdown();
    }

    @Test
    public void restEndpointCanProperlyBeInvoked() throws Exception {
        setupContext(MyConfig.class);
        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/endpoint", null);
        assertThat(replyAsString, is("MyBean"));
    }

    @Configuration
    @Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
        }
    }

    public static class MyBean {
        private List<String> listInvocationParams = new ArrayList<>();

        @OnRestRequest("/endpoint")
        public String singleParamMethod() throws Exception {
            return "MyBean";
        }
    }
}