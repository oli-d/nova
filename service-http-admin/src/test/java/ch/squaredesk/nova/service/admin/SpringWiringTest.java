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

import ch.squaredesk.nova.service.admin.messages.AdminMessage;
import ch.squaredesk.nova.service.admin.messages.Error;
import ch.squaredesk.nova.service.admin.messages.Info;
import ch.squaredesk.nova.service.admin.messages.Reply;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SpringWiringTest {

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    private ObjectMapper om = new ObjectMapper();

    private void setupContext(Class configClass) {
        ctx.register(configClass);
        ctx.refresh();
    }

    @AfterEach
    public void tearDown() throws Exception {
        ctx.getBean(AdminCommandServer.class).shutdown();
        int port = ctx.getBean("adminPort", Integer.class);
        HttpHelper.waitUntilNobodyListensOnPort(port,2, TimeUnit.SECONDS);
        System.out.println(">>>>>>>>>>> " + port + " <<<<<<<<<<<<<");
    }

    private String waitUntilAdminServerIsUpAndReturnUrl() throws Exception {
        int port = ctx.getBean("adminPort", Integer.class);
        String machine = ctx.getBean("adminInterfaceName", String.class);

        // FIXME: to wait until the CommAdapter has started. Otherwise the following line will be first and cause Commadapter to fail to start
        TimeUnit.SECONDS.sleep(3);
        HttpHelper.waitUntilSomebodyListensOnPort(port, 2, TimeUnit.SECONDS);
        return "rest://" + machine + ":" + port;
    }

    AdminMessage toAdminMessage(String reply) throws Exception {
        return toAdminMessage(reply, AdminMessage.class);
    }

    <T extends AdminMessage> T toAdminMessage(String reply, Class<T> targetClass) throws Exception {
        return om.readValue(reply, targetClass);
    }

    @Test
    void evenIfNothingRegisteredTheInfoCommandIsAlwaysSupported() throws Exception {
        setupContext(MyEmptyConfig.class);
        String serverUrl = waitUntilAdminServerIsUpAndReturnUrl();
        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/admin/info", null);

        AdminMessage reply = toAdminMessage(replyAsString);
        assertTrue(reply instanceof Reply);
        Info info = toAdminMessage(((Reply) reply).details, Info.class);
        assertThat(info.supportedCommands.length, is(1));
    }

    @Test
    public void adminCommandListCanProperlyBeRetrieved() throws Exception {
        setupContext(MyConfig.class);
        String serverUrl = waitUntilAdminServerIsUpAndReturnUrl();
        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/admin/info", null);


        AdminMessage reply = toAdminMessage(replyAsString);
        assertTrue(reply instanceof Reply);
        Info info = toAdminMessage(((Reply) reply).details, Info.class);
        assertThat(info.supportedCommands.length, is(3));
        assertThat(info.supportedCommands[0].url, is(serverUrl + "/admin/info"));
        assertThat(info.supportedCommands[1].url, is(serverUrl + "/admin/singleParamMethod"));
        assertThat(info.supportedCommands[1].parameters.length, is(1));
        assertThat(info.supportedCommands[1].parameters[0], is("firstParam"));
        assertThat(info.supportedCommands[2].url, is(serverUrl + "/admin/doubleParamMethod"));
        assertThat(info.supportedCommands[2].parameters.length, is(2));
        assertThat(info.supportedCommands[2].parameters[0], is("firstParam"));
        assertThat(info.supportedCommands[2].parameters[1], is("secondParam"));
    }

    @Test
    public void adminCommandDifferentThanInfoCanProperlyBeInvokedViaHttp() throws Exception {
        setupContext(MyConfig.class);
        MyBean sut = ctx.getBean(MyBean.class);
        String serverUrl = waitUntilAdminServerIsUpAndReturnUrl();
        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/admin/doubleParamMethod?firstParam=abc&secondParam=123", null);

        AdminMessage reply = toAdminMessage(replyAsString);
        assertTrue(reply instanceof Reply);
        assertThat(((Reply)reply).details, is("Success"));
        assertThat(sut.listInvocationParams.size(), is(2));
        assertThat(sut.listInvocationParams.get(0), is("abc"));
        assertThat(sut.listInvocationParams.get(1), is("123"));
    }

    @Test
    public void invokingAdminCommandWithTooFewParamsReturnsError() throws Exception {
        String serverUrl = waitUntilAdminServerIsUpAndReturnUrl();
        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/admin/doubleParamMethod?firstParam=abc","");
        AdminMessage reply = toAdminMessage(replyAsString);
        assertTrue(reply instanceof Error);
        assertThat(((Error)reply).message, is("Success"));
    }

    @Test
    public void invokingAdminCommandWithWrongParamNamesReturnsError() throws Exception {
        String serverUrl = waitUntilAdminServerIsUpAndReturnUrl();
        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/admin/singleParamMethod?firstParam=abc","");
        AdminMessage reply = toAdminMessage(replyAsString);
        assertTrue(reply instanceof Error);
        assertThat(((Error)reply).message, is("Success"));
    }

    @Test
    public void configCanBeRefreshed() throws Exception {
        fail("not implemenetd");
    }

    @Configuration
    @Import({AdminCommandEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyEmptyConfig  {
        @Bean()
        public MyEmptyBean myBean() {
            return new MyEmptyBean();
        }
    }

    public static class MyEmptyBean {
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