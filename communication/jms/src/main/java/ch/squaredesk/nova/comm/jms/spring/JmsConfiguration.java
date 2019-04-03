package ch.squaredesk.nova.comm.jms.spring;

import ch.squaredesk.nova.comm.jms.JmsAdapter;

public class JmsConfiguration {
    public final int defaultMessageDeliveryMode;
    public final int defaultMessagePriority;
    public final long defaultMessageTimeToLive;
    public final int defaultJmsRpcTimeoutInSeconds;
    public final int consumerSessionAckMode;
    public final boolean consumerSessionTransacted;
    public final int producerSessionAckMode;
    public final boolean producerSessionTransacted;
    public final String jmsAdapterIdentifier;

    private JmsConfiguration (Builder builder) {
        this.defaultMessageDeliveryMode = builder.defaultMessageDeliveryMode;
        this.defaultMessagePriority = builder.defaultMessagePriority;
        this.defaultMessageTimeToLive = builder.defaultMessageTimeToLive;
        this.defaultJmsRpcTimeoutInSeconds = builder.defaultJmsRpcTimeoutInSeconds;
        this.consumerSessionAckMode = builder.consumerSessionAckMode;
        this.consumerSessionTransacted = builder.consumerSessionTransacted;
        this.producerSessionAckMode = builder.producerSessionAckMode;
        this.producerSessionTransacted = builder.producerSessionTransacted;
        this.jmsAdapterIdentifier = builder.jmsAdapterIdentifier;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int defaultMessageDeliveryMode;
        private int defaultMessagePriority;
        private long defaultMessageTimeToLive;
        private int defaultJmsRpcTimeoutInSeconds;
        private int consumerSessionAckMode;
        private boolean consumerSessionTransacted;
        private int producerSessionAckMode;
        private boolean producerSessionTransacted;
        private String jmsAdapterIdentifier;

        private Builder() {
        }

        public Builder setIdentifier(String value) {
            return this;
        }

        public Builder setDefaultMessageDeliveryMode(int value) {
            return this;
        }

        public Builder setDefaultMessagePriority(int value) {
            return this;
        }

        public Builder setDefaultMessageTimeToLive(long value) {
            return this;
        }

        public Builder setDefaultRpcTimeoutInSeconds(int value) {
            return this;
        }

        public Builder setConsumerSessionAckMode(int value) {
            return this;
        }

        public Builder setConsumerSessionTransacted(boolean value) {
            return this;
        }

        public Builder setProducerSessionAckMode(int value) {
            return this;
        }

        public Builder setProducerSessionTransacted(boolean value) {
            return this;
        }

        public JmsConfiguration build() {
            return new JmsConfiguration(this);
        }
    }
}
