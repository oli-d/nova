package ch.squaredesk.nova.comm.kafka.spring;

import ch.squaredesk.nova.comm.kafka.KafkaAdapter;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        KafkaEnablingConfiguration.class,
        KafkaEnablingConfigurationTest.AdapterDisablingConfig.class,
        NovaProvidingConfiguration.class})
class KafkaEnablingConfigurationTest {
    @Autowired
    KafkaAdapterSettings kafkaAdapterSettings;

    @BeforeAll
    static void setup() {
        System.setProperty("NOVA.KAFKA.ADAPTER_IDENTIFIER", "myId");
        System.setProperty("NOVA.KAFKA.SERVER_ADDRESS", "server:port");
        System.setProperty("NOVA.KAFKA.POLL_TIMEOUT_IN_MILLISECONDS", "123");
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("NOVA.KAFKA.ADAPTER_IDENTIFIER");
        System.clearProperty("NOVA.KAFKA.SERVER_ADDRESS");
        System.clearProperty("NOVA.KAFKA.POLL_TIMEOUT_IN_MILLISECONDS");
    }

    @Test
    void environmentVariablesProperlyParsed() {
        assertThat(kafkaAdapterSettings.identifier, is("myId"));
        assertThat(kafkaAdapterSettings.serverAddress, is("server:port"));
        assertThat(kafkaAdapterSettings.pollTimeoutInMilliseconds, is(123L));
    }

    @Configuration
    static class AdapterDisablingConfig {
        @Bean("kafkaAdapter")
        KafkaAdapter kafkaAdapter() { return null; }
    }
}