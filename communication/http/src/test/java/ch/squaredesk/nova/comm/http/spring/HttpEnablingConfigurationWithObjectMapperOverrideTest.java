/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.comm.http.HttpAdapter;
import ch.squaredesk.nova.comm.http.HttpRequestSender;
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.comm.http.RpcInvocation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

@Tag("medium")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HttpEnablingConfigurationWithObjectMapperOverrideTest.ConfigWithObjectMapper.class})
class HttpEnablingConfigurationWithObjectMapperOverrideTest {
    @Autowired
    private HttpServerSettings serverConfiguration;
    @Autowired
    private HttpAdapter adapter;
    private String adapterBaseUrl;

    @BeforeEach
    void setup() throws Exception  {
        adapterBaseUrl = "http://localhost:" + serverConfiguration.port;
        adapter.start();
    }

    @AfterEach
    void shutdownServer() throws Exception  {
        if (adapter!=null) {
            adapter.shutdown();
        }
    }


    public static class MyDto {
        public final BigDecimal value;

        @JsonCreator
        MyDto(@JsonProperty("value") BigDecimal value) {
            this.value = value;
        }
    }

    @Test
    void providedObjectMapperIsUsed() throws Exception {
        String requestDestination = "/objectMapperTest";
        Flowable<RpcInvocation<MyDto>> requests = adapter.requests(requestDestination, MyDto.class);
        requests.subscribe(invocation -> {
            invocation.complete(invocation.request.message.value);
        });

        // send MyDTO instance, but send BigDecimal value as String, not number
        String reply = HttpRequestSender.sendPostRequest(
                adapterBaseUrl + "/" + requestDestination,
                "{\"value\":\"123\"}").replyMessage;

        // the request handler sent back the BigDecimal 123, so we expect "123" back
        MatcherAssert.assertThat(reply, Matchers.is("\"123\""));
    }

    @Configuration
    @Import(HttpEnablingConfiguration.class)
    public static class ConfigWithObjectMapper {
        @Bean("httpServerPort")
        public Integer httpServerPort() {
            return PortFinder.findFreePort();
        }

        @Bean("httpObjectMapper")
        public ObjectMapper httpObjectMapper() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configOverride(BigDecimal.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
            return objectMapper;
        }
    }
}