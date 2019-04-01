package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.*;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageTranscriber;
import ch.squaredesk.nova.comm.sending.OutgoingMessageTranscriber;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("medium")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HttpEnablingConfigurationWithOverridesTest.ConfigWithTranscriber.class})
class HttpEnablingConfigurationWithOverridesTest {
    @Autowired
    private HttpServerConfiguration serverConfiguration;
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
    void providedMessageTranscriberIsUsed() throws Exception {
        String requestDestination = "/transcriberTest";
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


    @Import({HttpEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class ConfigWithTranscriber {
        @Bean("httpServerPort")
        public Integer httpServerPort() {
            return PortFinder.findFreePort();
        }

        @Bean("httpMessageTranscriber")
        public MessageTranscriber<String> httpMessageTranscriber() {
            ObjectMapper om = (new ObjectMapper()).findAndRegisterModules().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
            om.configOverride(BigDecimal.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));

            return new MessageTranscriber<>(om::writeValueAsString, om::readValue);
        }
    }
}