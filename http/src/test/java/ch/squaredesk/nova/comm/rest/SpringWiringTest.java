/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.comm.rest.grizzly.HelloWorldResource;
import ch.squaredesk.nova.service.admin.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class SpringWiringTest {

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    private String serverUrl;

    private void setupContext(Class configClass) throws Exception {
        ctx.register(configClass);
        ctx.refresh();
        serverUrl = "http://" + ctx.getBean("restInterfaceName") + ":" + ctx.getBean("restPort");
//        ctx.getBean(ResourceConfig.class).packages("ch.squaredesk.nova.comm.rest");
//        Resource.Builder b = Resource.builder();
//        b.addChildResource(Resource.from(MyBean.class));
//        ctx.getBean(ResourceConfig.class).registerResources(Resource.from(HelloWorldResource.class),
//                Resource.from(MyBeanStatic.class));
        ctx.getBean(HttpServer.class).start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        ctx.getBean(HttpServer.class).shutdown();
    }

    @Test
    public void restEndpointCanProperlyBeInvoked() throws Exception {
        setupContext(MyConfig.class);
//        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/admin/info", null);
//        assertThat(replyAsString, is("xxxx"));
        TimeUnit.SECONDS.sleep(1000);
    }

    @Configuration
    @Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyConfig  {
        @Bean
        public MyBeanStatic myBean() {
            return new MyBeanStatic();
        }
    }

    @Path("/")
    public static class MyBeanStatic {
        private List<String> listInvocationParams = new ArrayList<>();

        @GET
        @Path("/spring")
        @Produces(MediaType.TEXT_PLAIN)
        public String singleParamMethod() throws Exception {
            System.out.println("I am triggered :-)");
            return "Success again";
        }
    }
}