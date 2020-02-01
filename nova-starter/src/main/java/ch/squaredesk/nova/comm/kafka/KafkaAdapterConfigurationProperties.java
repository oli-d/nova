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

package ch.squaredesk.nova.comm.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties("nova.kafka")
public class KafkaAdapterConfigurationProperties {
    /** The (optional) identifier of the HTTP adapter */
    private String adapterIdentifier;
    private String serverAddress;
    private long pollTimeoutInMs;
    private String brokerClientId;
    private String consumerGroupId;
    private Properties consumerProperties;
    private Properties producerProperties;

    public String getBrokerClientId() {
        return brokerClientId;
    }

    public void setBrokerClientId(String brokerClientId) {
        this.brokerClientId = brokerClientId;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public Properties getConsumerProperties() {
        return consumerProperties;
    }

    public void setConsumerProperties(Properties consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    public Properties getProducerProperties() {
        return producerProperties;
    }

    public void setProducerProperties(Properties producerProperties) {
        this.producerProperties = producerProperties;
    }

    public long getPollTimeoutInMs() {
        return pollTimeoutInMs;
    }

    public void setPollTimeoutInMs(long pollTimeoutInMs) {
        this.pollTimeoutInMs = pollTimeoutInMs;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getAdapterIdentifier() {
        return adapterIdentifier;
    }

    public void setAdapterIdentifier(String adapterIdentifier) {
        this.adapterIdentifier = adapterIdentifier;
    }
}
