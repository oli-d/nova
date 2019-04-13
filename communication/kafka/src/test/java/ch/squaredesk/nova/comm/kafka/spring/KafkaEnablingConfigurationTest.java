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
        KafkaEnablingConfigurationTest.AdapterDisablingConfig.class,
        KafkaEnablingConfiguration.class})
class KafkaEnablingConfigurationTest {
    @Autowired
    KafkaAdapterSettings kafkaAdapterSettings;

    @BeforeAll
    static void setup() {
        System.setProperty(KafkaEnablingConfiguration.BeanIdentifiers.ADAPTER_IDENTIFIER, "myId");
        System.setProperty(KafkaEnablingConfiguration.BeanIdentifiers.SERVER_ADDRESS, "localhost:4567");
        System.setProperty(KafkaEnablingConfiguration.BeanIdentifiers.POLL_TIMEOUT_IN_MS, "123");
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty(KafkaEnablingConfiguration.BeanIdentifiers.ADAPTER_IDENTIFIER);
        System.clearProperty(KafkaEnablingConfiguration.BeanIdentifiers.SERVER_ADDRESS);
        System.clearProperty(KafkaEnablingConfiguration.BeanIdentifiers.POLL_TIMEOUT_IN_MS);
    }

    @Test
    void environmentVariablesProperlyParsed() {
        assertThat(kafkaAdapterSettings.identifier, is("myId"));
        assertThat(kafkaAdapterSettings.serverAddress, is("localhost:4567"));
        assertThat(kafkaAdapterSettings.pollTimeoutInMilliseconds, is(123L));
    }

    @Configuration
    static class AdapterDisablingConfig {
        @Bean(KafkaEnablingConfiguration.BeanIdentifiers.ADAPTER)
        KafkaAdapter kafkaAdapter() { return null; }
    }
}