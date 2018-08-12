/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest.annotation;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpRequestMethod;
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.comm.http.MediaType;
import ch.squaredesk.nova.comm.http.RpcServer;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

class FormDataTest {

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    private String serverUrl;

    private void setupContext(Class configClass) throws Exception {
        ctx.register(configClass);
        ctx.refresh();
        HttpServerConfiguration cfg = ctx.getBean(HttpServerConfiguration.class);
        serverUrl = "http://" + cfg.interfaceName + ":" + cfg.port;
    }

    @AfterEach
    void tearDown() throws Exception {
        ctx.close();
    }

    @Test
    void binaryFileCanBeUploadedAsFormData() throws Exception {
        setupContext(MyConfig.class);
        MyBean myBean = ctx.getBean(MyBean.class);

        String fullPath = getClass().getResource("/someBinaryFile.ser").getFile();
        String curlCmd = "curl -X POST -H 'string: headerParam' -H 'Content-Type: application/octet-stream' --data-binary '@" + fullPath + "' localhost:10000/xlsUpload";
//        Process p = Runtime.getRuntime().exec(curlCmd);
//        int x = p.waitFor();
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
//            while (reader.ready()) System.out.println(reader.readLine());
//        }
//        System.out.println("Curl exited with " + x);
//
//        assertThat(myBean.stringData, is("headerParam"));
//        assertThat(myBean.binaryData.length, is(9));
        Thread.sleep(1000000000);
    }

    @Configuration
    @Order
    @Import({RestEnablingConfiguration.class})
    public static class MyMixedConfig  {
        @Autowired
        ApplicationContext applicationContext;

        @Autowired
        public Nova nova;

        @Bean
        public MyBean myBean() {
            return new MyBean();
        }
    }

    @Configuration
    @Import({NovaProvidingConfiguration.class, RestEnablingConfiguration.class})
    public static class MyConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
        }

    }

    public static class MyBean {
        private byte[] binaryData;
        private String stringData;

        @OnRestRequest(value = "/xlsUpload", requestMethod = HttpRequestMethod.POST, consumes = MediaType.APPLICATION_OCTET_STREAM, produces = MediaType.TEXT_PLAIN)
        public String restHandler(byte[] binaryData, @HeaderParam("string") String stringData)  {
            System.out.println("I was called!!! " + Arrays.toString(binaryData));
            this.binaryData = binaryData;
            this.stringData = stringData;
            return "#bytes=" + binaryData.length + ", header=" + stringData + "\n";
        }
    }
}