package ch.squaredesk.nova.comm.jms.spring;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.jms.Message;
import javax.jms.Session;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JmsEnablingConfiguration.class)
class JmsEnablingConfigurationEnvironmentVariableTest {
    @Autowired
    JmsConfiguration jmsConfiguration;

    @BeforeAll
    static void setup() {
        System.setProperty("NOVA.JMS.DEFAULT_MESSAGE_DELIVERY_MODE", String.valueOf(Message.DEFAULT_DELIVERY_MODE));
        System.setProperty("NOVA.JMS.DEFAULT_MESSAGE_PRIORITY", "DEFAULT_PRIORITY");
        System.setProperty("NOVA.JMS.DEFAULT_MESSAGE_TIME_TO_LIVE", "DEFAULT_TIME_TO_LIVE");
        System.setProperty("NOVA.JMS.CONSUMER_SESSION_ACK_MODE", "CLIENT_ACKNOWLEDGE");
        System.setProperty("NOVA.JMS.CONSUMER_SESSION_TRANSACTED", "true");
        System.setProperty("NOVA.JMS.PRODUCER_SESSION_ACK_MODE", String.valueOf(Session.DUPS_OK_ACKNOWLEDGE));
        System.setProperty("NOVA.JMS.PRODUCER_SESSION_TRANSACTED", "true");
        System.setProperty("NOVA.JMS.DEFAULT_RPC_TIMEOUT_IN_SECONDS", "11");
        System.setProperty("NOVA.JMS.ADAPTER_IDENTIFIER", "myId");
    }

    @Test
    void environmentVariablesProperlyParsed() {
        assertThat(jmsConfiguration.defaultMessageDeliveryMode, is(Message.DEFAULT_DELIVERY_MODE));
        assertThat(jmsConfiguration.defaultMessagePriority, is(Message.DEFAULT_PRIORITY));
        assertThat(jmsConfiguration.defaultMessageTimeToLive, is((int)Message.DEFAULT_TIME_TO_LIVE));
        assertThat(jmsConfiguration.consumerSessionAckMode, is(Session.CLIENT_ACKNOWLEDGE));
        assertThat(jmsConfiguration.consumerSessionTransacted, is(true));
        assertThat(jmsConfiguration.producerSessionAckMode, is(Session.DUPS_OK_ACKNOWLEDGE));
        assertThat(jmsConfiguration.producerSessionTransacted, is(true));
        assertThat(jmsConfiguration.defaultJmsRpcTimeoutInSeconds, is(11));
        assertThat(jmsConfiguration.jmsAdapterIdentifier, is("myId"));
    }

}