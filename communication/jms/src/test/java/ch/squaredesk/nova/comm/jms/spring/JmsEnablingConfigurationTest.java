package ch.squaredesk.nova.comm.jms.spring;

import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.Session;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        JmsEnablingConfiguration.class,
        JmsEnablingConfigurationTest.TestConfig.class,
        NovaProvidingConfiguration.class})
class JmsEnablingConfigurationTest {
    @Autowired
    JmsAdapterSettings jmsAdapterSettings;

    @BeforeAll
    static void setup() {
        System.setProperty(JmsEnablingConfiguration.BeanIdentifiers.DEFAULT_MESSAGE_DELIVERY_MODE, String.valueOf(Message.DEFAULT_DELIVERY_MODE));
        System.setProperty(JmsEnablingConfiguration.BeanIdentifiers.DEFAULT_MESSAGE_PRIORITY, "DEFAULT_PRIORITY");
        System.setProperty(JmsEnablingConfiguration.BeanIdentifiers.DEFAULT_MESSAGE_TIME_TO_LIVE, "DEFAULT_TIME_TO_LIVE");
        System.setProperty(JmsEnablingConfiguration.BeanIdentifiers.CONSUMER_SESSION_ACK_MODE, "CLIENT_ACKNOWLEDGE");
        System.setProperty(JmsEnablingConfiguration.BeanIdentifiers.PRODUCER_SESSION_ACK_MODE, String.valueOf(Session.DUPS_OK_ACKNOWLEDGE));
        System.setProperty(JmsEnablingConfiguration.BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS, "11");
        System.setProperty(JmsEnablingConfiguration.BeanIdentifiers.ADAPTER_IDENTIFIER, "myId");
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty(JmsEnablingConfiguration.BeanIdentifiers.DEFAULT_MESSAGE_DELIVERY_MODE);
        System.clearProperty(JmsEnablingConfiguration.BeanIdentifiers.DEFAULT_MESSAGE_PRIORITY);
        System.clearProperty(JmsEnablingConfiguration.BeanIdentifiers.DEFAULT_MESSAGE_TIME_TO_LIVE);
        System.clearProperty(JmsEnablingConfiguration.BeanIdentifiers.CONSUMER_SESSION_ACK_MODE);
        System.clearProperty(JmsEnablingConfiguration.BeanIdentifiers.PRODUCER_SESSION_ACK_MODE);
        System.clearProperty(JmsEnablingConfiguration.BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS);
        System.clearProperty(JmsEnablingConfiguration.BeanIdentifiers.ADAPTER_IDENTIFIER);
    }

    @Test
    void environmentVariablesProperlyParsed() {
        assertThat(jmsAdapterSettings.defaultMessageDeliveryMode, is(Message.DEFAULT_DELIVERY_MODE));
        assertThat(jmsAdapterSettings.defaultMessagePriority, is(Message.DEFAULT_PRIORITY));
        assertThat(jmsAdapterSettings.defaultMessageTimeToLive, is(Message.DEFAULT_TIME_TO_LIVE));
        assertThat(jmsAdapterSettings.consumerSessionAckMode, is(Session.CLIENT_ACKNOWLEDGE));
        assertThat(jmsAdapterSettings.producerSessionAckMode, is(Session.DUPS_OK_ACKNOWLEDGE));
        assertThat(jmsAdapterSettings.defaultJmsRpcTimeoutInSeconds, is(11));
        assertThat(jmsAdapterSettings.jmsAdapterIdentifier, is("myId"));
    }

    @Configuration
    public static class TestConfig {
        @Bean(JmsEnablingConfiguration.BeanIdentifiers.CONNECTION_FACTORY)
        ConnectionFactory connectionFactory() {
            return new ActiveMQConnectionFactory();
        }
    }
}