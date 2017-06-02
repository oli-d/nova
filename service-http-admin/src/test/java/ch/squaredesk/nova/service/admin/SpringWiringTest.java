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

import ch.squaredesk.nova.service.admin.messages.*;
import ch.squaredesk.nova.service.admin.messages.Error;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SpringWiringTest {

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    private ObjectMapper om = new ObjectMapper();

    @BeforeEach
    public void setup() {
        ctx.register(MyConfig.class);
        ctx.refresh();
    }

    @AfterEach
    public void tearDown() {
        // TODO: shutdown
    }

//    @Test
//    void evenIfNothingRegisteredTheInfoCommandIsAlwaysSupported() {
//        List<AdminCommandConfig> configs = sut.getConfigs();
//        assertThat(configs.size(), is(1));
//        assertThat(configs.get(0).methodToInvoke.getName(), is("config"));
//        assertThat(configs.get(0).parameterNames, is(new String[0]));
//
//        Info info = sut.getInfo();
//        assertNotNull(info);
//        assertThat(info.supportedCommands.length, is(1));
//        assertThat(info.supportedCommands[0].url, is(urlCalculator.urlFor(configs.get(0))));
//    }

    @Test
    public void adminCommandListCanProperlyBeRetrieved() throws Exception {
        MyBean sut = ctx.getBean(MyBean.class);

        HttpHelper.waitUntilSomebodyListensOnPort(8888, 2, TimeUnit.SECONDS);
        String replyAsString = HttpHelper.getResponseBody("/127.0.0.1:8888/admin/info","");
        Info info = om.readValue(replyAsString, Info.class);
        assertThat(info.supportedCommands.length, is(2));
        assertThat(info.supportedCommands[0].url, is("/admin/singleParamMethod"));
        assertThat(info.supportedCommands[0].parameters.length, is(1));
        assertThat(info.supportedCommands[0].parameters[0], is("firstParam"));
        assertThat(info.supportedCommands[1].url, is("/admin/doubleParamMethod"));
        assertThat(info.supportedCommands[1].parameters.length, is(2));
        assertThat(info.supportedCommands[1].parameters[0], is("firstParam"));
        assertThat(info.supportedCommands[1].parameters[1], is("secondParam"));
    }

    @Test
    public void adminCommandCanProperlyBeInvokedViaHttp() throws Exception {
        MyBean sut = ctx.getBean(MyBean.class);

        HttpHelper.waitUntilSomebodyListensOnPort(8888, 2, TimeUnit.SECONDS);
        String replyAsString = HttpHelper.getResponseBody("/127.0.0.1:8888/admin/doubleParamMethod?firstParam=abc&secondParam=123","");
        AdminMessage reply = om.readValue(replyAsString, AdminMessage.class);
        assertTrue(reply instanceof Reply);
        assertThat(((Reply)reply).details, is("Success"));
        assertThat(sut.listInvocationParams.size(), is(2));
        assertThat(sut.listInvocationParams.get(0), is("abc"));
        assertThat(sut.listInvocationParams.get(1), is("123"));
    }

    @Test
    public void invokingAdminCommandWithTooFewParamsReturnsError() throws Exception {
        HttpHelper.waitUntilSomebodyListensOnPort(8888, 2, TimeUnit.SECONDS);
        String replyAsString = HttpHelper.getResponseBody("/127.0.0.1:8888/admin/doubleParamMethod?firstParam=abc","");
        AdminMessage reply = om.readValue(replyAsString, AdminMessage.class);
        assertTrue(reply instanceof Error);
        assertThat(((Error)reply).details, is("Success"));
    }

    @Test
    public void invokingAdminCommandWithWrongParamNamesReturnsError() throws Exception {
        HttpHelper.waitUntilSomebodyListensOnPort(8888, 2, TimeUnit.SECONDS);
        String replyAsString = HttpHelper.getResponseBody("/127.0.0.1:8888/admin/singleParamMethod?firstParam=abc","");
        AdminMessage reply = om.readValue(replyAsString, AdminMessage.class);
        assertTrue(reply instanceof Error);
        assertThat(((Error)reply).details, is("Success"));
    }

    @Test
    public void configCanBeRefreshed() throws Exception {
        fail("not implemenetd");
    }

    @Configuration
    @Import({AdminCommandEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyConfig  {
        @Bean()
        public MyBean myBean() {
            return new MyBean();
        }
    }

    public static class MyBean {
        private List<String> listInvocationParams = new ArrayList<>();

        @OnAdminCommand("firstParam")
        public String singleParamMethod(String x) throws Exception {
            listInvocationParams.add(x);
            return "Success1";
        }

        @OnAdminCommand({"firstParam", "secondParam"})
        public String doubleParamMethod(String x, String y) throws Exception {
            listInvocationParams.add(x);
            listInvocationParams.add(y);
            return "Success2";
        }
    }
}