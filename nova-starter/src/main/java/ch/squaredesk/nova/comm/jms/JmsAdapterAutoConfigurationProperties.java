/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.jms;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.jms.Message;
import javax.jms.Session;

@ConfigurationProperties("nova.jms")
public class JmsAdapterAutoConfigurationProperties {
    /** The default delivery mode for messages sent to the broker */
    private int defaultMessageDeliveryMode = Message.DEFAULT_DELIVERY_MODE;
    /** The default priority for messages sent to the broker */
    private int defaultMessagePriority = Message.DEFAULT_PRIORITY;
    /** The default time to live for messages sent to the broker */
    private long defaultMessageTimeToLive = Message.DEFAULT_TIME_TO_LIVE;
    /** The default amount of seconds to wait for an RPC reply before signalling a timeout */
    private int defaultJmsRpcTimeoutInSeconds;
    /** The default consumer session ACK mode */
    private int consumerSessionAckMode = Session.AUTO_ACKNOWLEDGE;
    /** Is the consumer session transacted? */
    private boolean consumerSessionTransacted = false;
    /** The default producer session ACK mode */
    private int producerSessionAckMode;
    /** Is the producer session transacted? */
    private boolean producerSessionTransacted = false;
    /** The (optional) adapter identifier */
    private String adapterIdentifier;
    /** Should the adapter be automatically started after the bean context was initialized? */
    private boolean autoStartAdapter = true;

    public int getDefaultMessageDeliveryMode() {
        return defaultMessageDeliveryMode;
    }

    public void setDefaultMessageDeliveryMode(int defaultMessageDeliveryMode) {
        this.defaultMessageDeliveryMode = defaultMessageDeliveryMode;
    }

    public int getDefaultMessagePriority() {
        return defaultMessagePriority;
    }

    public void setDefaultMessagePriority(int defaultMessagePriority) {
        this.defaultMessagePriority = defaultMessagePriority;
    }

    public long getDefaultMessageTimeToLive() {
        return defaultMessageTimeToLive;
    }

    public void setDefaultMessageTimeToLive(long defaultMessageTimeToLive) {
        this.defaultMessageTimeToLive = defaultMessageTimeToLive;
    }

    public int getDefaultJmsRpcTimeoutInSeconds() {
        return defaultJmsRpcTimeoutInSeconds;
    }

    public void setDefaultJmsRpcTimeoutInSeconds(int defaultJmsRpcTimeoutInSeconds) {
        this.defaultJmsRpcTimeoutInSeconds = defaultJmsRpcTimeoutInSeconds;
    }

    public int getConsumerSessionAckMode() {
        return consumerSessionAckMode;
    }

    public void setConsumerSessionAckMode(int consumerSessionAckMode) {
        this.consumerSessionAckMode = consumerSessionAckMode;
    }

    public boolean isConsumerSessionTransacted() {
        return consumerSessionTransacted;
    }

    public void setConsumerSessionTransacted(boolean consumerSessionTransacted) {
        this.consumerSessionTransacted = consumerSessionTransacted;
    }

    public int getProducerSessionAckMode() {
        return producerSessionAckMode;
    }

    public void setProducerSessionAckMode(int producerSessionAckMode) {
        this.producerSessionAckMode = producerSessionAckMode;
    }

    public boolean isProducerSessionTransacted() {
        return producerSessionTransacted;
    }

    public void setProducerSessionTransacted(boolean producerSessionTransacted) {
        this.producerSessionTransacted = producerSessionTransacted;
    }

    public String getAdapterIdentifier() {
        return adapterIdentifier;
    }

    public void setAdapterIdentifier(String adapterIdentifier) {
        this.adapterIdentifier = adapterIdentifier;
    }

    public boolean isAutoStartAdapter() {
        return autoStartAdapter;
    }

    public void setAutoStartAdapter(boolean autoStartAdapter) {
        this.autoStartAdapter = autoStartAdapter;
    }
}
