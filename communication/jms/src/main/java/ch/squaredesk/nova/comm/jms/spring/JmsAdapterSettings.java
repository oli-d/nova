package ch.squaredesk.nova.comm.jms.spring;

public class JmsAdapterSettings {
    public final int defaultMessageDeliveryMode;
    public final int defaultMessagePriority;
    public final long defaultMessageTimeToLive;
    public final int defaultJmsRpcTimeoutInSeconds;
    public final int consumerSessionAckMode;
    public final boolean consumerSessionTransacted;
    public final int producerSessionAckMode;
    public final boolean producerSessionTransacted;
    public final String jmsAdapterIdentifier;

    private JmsAdapterSettings(Builder builder) {
        this.jmsAdapterIdentifier = builder.jmsAdapterIdentifier;
        this.defaultMessageDeliveryMode = builder.defaultMessageDeliveryMode;
        this.defaultMessagePriority = builder.defaultMessagePriority;
        this.defaultMessageTimeToLive = builder.defaultMessageTimeToLive;
        this.defaultJmsRpcTimeoutInSeconds = builder.defaultJmsRpcTimeoutInSeconds;
        this.consumerSessionAckMode = builder.consumerSessionAckMode;
        this.producerSessionAckMode = builder.producerSessionAckMode;
        this.consumerSessionTransacted = builder.consumerSessionTransacted;
        this.producerSessionTransacted = builder.producerSessionTransacted;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String jmsAdapterIdentifier;
        private int defaultMessageDeliveryMode;
        private int defaultMessagePriority;
        private long defaultMessageTimeToLive;
        private int defaultJmsRpcTimeoutInSeconds;
        private int consumerSessionAckMode;
        private int producerSessionAckMode;
        private final boolean consumerSessionTransacted = false;
        private final boolean producerSessionTransacted = false;

        private Builder() {
        }

        public Builder setIdentifier(String value) {
            this.jmsAdapterIdentifier = value;
            return this;
        }

        public Builder setDefaultMessageDeliveryMode(int value) {
            this.defaultMessageDeliveryMode = value;
            return this;
        }

        public Builder setDefaultMessagePriority(int value) {
            this.defaultMessagePriority = value;
            return this;
        }

        public Builder setDefaultMessageTimeToLive(long value) {
            this.defaultMessageTimeToLive = value;
            return this;
        }

        public Builder setDefaultRpcTimeoutInSeconds(int value) {
            this.defaultJmsRpcTimeoutInSeconds = value;
            return this;
        }

        public Builder setConsumerSessionAckMode(int value) {
            this.consumerSessionAckMode = value;
            return this;
        }

        public Builder setProducerSessionAckMode(int value) {
            this.producerSessionAckMode = value;
            return this;
        }

        public JmsAdapterSettings build() {
            return new JmsAdapterSettings(this);
        }
    }
}
