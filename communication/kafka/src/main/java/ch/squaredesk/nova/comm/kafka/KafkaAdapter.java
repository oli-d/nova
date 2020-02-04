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

import ch.squaredesk.nova.comm.CommAdapter;
import ch.squaredesk.nova.comm.CommAdapterBuilder;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class KafkaAdapter extends CommAdapter<String> {
    private static final Logger logger = LoggerFactory.getLogger(KafkaAdapter.class);

    private final MessageSender messageSender;
    private final MessageReceiver messageReceiver;


    KafkaAdapter(MessageSender messageSender,
                 MessageReceiver messageReceiver,
                 MessageTranscriber<String> messageTranscriber,
                 Metrics metrics) {
        super(messageTranscriber, metrics);
        this.messageReceiver = messageReceiver;
        this.messageSender = messageSender;
    }

    /////////////////////////////////
    //                             //
    // simple send related methods //
    //                             //
    /////////////////////////////////
    public Single<OutgoingMessageMetaData> sendMessage(String destination, String message) {
        SendInfo sendInfo = new SendInfo();
        OutgoingMessageMetaData meta = new OutgoingMessageMetaData(destination, sendInfo);
        return messageSender.send(message, meta);
    }

    public <T> Single<OutgoingMessageMetaData> sendMessage(String destination, T message) {
        Function<T, String> transcriber = messageTranscriber.getOutgoingMessageTranscriber((Class<T>)message.getClass());
        return sendMessage(destination, message, transcriber);
    }

    public <T> Single<OutgoingMessageMetaData> sendMessage(String destination, T message, Function<T, String> transcriber) {
        requireNonNull(message, "message must not be null");
        SendInfo sendInfo = new SendInfo();
        OutgoingMessageMetaData meta = new OutgoingMessageMetaData(destination, sendInfo);
        return this.messageSender.send(message, meta, transcriber)
        /*.doOnError(t -> examineSendExceptionForDeadDestinationAndInformListener(t, destination))*/;
    }

    //////////////////////////////////
    //                              //
    // subscription related methods //
    //                              //
    //////////////////////////////////
    public Flowable<String> messages (String destination) {
        return messageReceiver.messages(destination).map(incomingMessage -> incomingMessage.message);
    }

    public <T> Flowable<T> messages (String destination, Class<T> messageType) {
        return messages(destination, messageTranscriber.getIncomingMessageTranscriber(messageType));
    }

    public <T> Flowable<T> messages (String destination, Function<String,T> messageTranscriber) {
        return messageReceiver.messages(destination, messageTranscriber).map(incomingMessage -> incomingMessage.message);
    }


    /////////////////////////
    //                     //
    //  lifecycle  methods //
    //                     //
    /////////////////////////
    public void shutdown() {
        if (messageReceiver != null) {
            messageReceiver.shutdown();
        }
        logger.info("KafkaAdapter shut down");
    }


    /////////////////////////
    //                     //
    //    the  Builder     //
    //                     //
    /////////////////////////
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends CommAdapterBuilder<String, KafkaAdapter> {
        private String serverAddress;
        private String brokerClientId;
        private String consumerGroupId;
        private String identifier;
        private MessageSender messageSender;
        private MessageReceiver messageReceiver;
        private Properties consumerProperties = new Properties();
        private Properties producerProperties = new Properties();
        private Duration pollTimeout = Duration.ofSeconds(1);

        private Builder() {
        }

        public Builder setMessagePollingTimeout(Duration pollTimeout) {
            this.pollTimeout = pollTimeout;
            return this;
        }

        public Builder setConsumerProperties(Properties consumerProperties) {
            if (consumerProperties!=null) {
                this.consumerProperties.putAll(consumerProperties);
            }
            return this;
        }

        private Builder addProperty(Properties target, String key, String value) {
            requireNonNull(key, "property key must not be null");
            requireNonNull(value, "value for property " + key + " must not be null");
            target.setProperty(key, value);
            return this;
        }

        public Builder addConsumerProperty(String key, String value) {
            return addProperty(consumerProperties, key, value);
        }

        public Builder addProducerProperty(String key, String value) {
            return addProperty(producerProperties, key, value);
        }

        public Builder setProducerProperties(Properties producerProperties) {
            if (producerProperties != null) {
                this.producerProperties.putAll(producerProperties);
            }
            return this;
        }

        public Builder setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder setBrokerClientId(String value) {
            this.brokerClientId = value;
            return this;
        }

        public Builder setConsumerGroupId(String value) {
            this.consumerGroupId = value;
            return this;
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setMessageSender(MessageSender messageSender) {
            this.messageSender = messageSender;
            return this;
        }

        public Builder setMessageReceiver(MessageReceiver messageReceiver) {
            this.messageReceiver = messageReceiver;
            return this;
        }

        @Override
        public void validate() {
            requireNonNull(serverAddress,"serverAddress must be provided");
            requireNonNull(metrics,"metrics must be provided");
            if (consumerProperties==null) consumerProperties = new Properties();
            if (producerProperties==null) producerProperties = new Properties();
        }

        public KafkaAdapter createInstance() {
            String clientId = brokerClientId == null ? "KafkaAdapter-"+UUID.randomUUID() : brokerClientId;
            String groupId = consumerGroupId == null ? consumerProperties.getProperty(ConsumerConfig.GROUP_ID_CONFIG) : consumerGroupId;
            requireNonNull(groupId,"consumerGroupId must be provided");

            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddress);
            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.CLIENT_ID_CONFIG, clientId);
            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.GROUP_ID_CONFIG, groupId);

            setPropertyIfNotPresent(producerProperties, ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddress);
            setPropertyIfNotPresent(producerProperties, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            setPropertyIfNotPresent(producerProperties, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            setPropertyIfNotPresent(producerProperties, ProducerConfig.CLIENT_ID_CONFIG, clientId);

            if (messageReceiver == null) {
                messageReceiver = new MessageReceiver(identifier, consumerProperties, pollTimeout, metrics);
            }
            if (messageSender == null) {
                messageSender = new MessageSender(identifier, producerProperties, metrics);
            }
            if (messageTranscriber == null) {
                messageTranscriber = new DefaultMessageTranscriberForStringAsTransportType();
            }
            return new KafkaAdapter(this.messageSender, this.messageReceiver, messageTranscriber, metrics);
        }

        private static void setPropertyIfNotPresent (Properties props, String key, String value) {
            if (value != null && !props.containsKey(key)) {
                props.setProperty(key, value);
            }
        }
    }
}
