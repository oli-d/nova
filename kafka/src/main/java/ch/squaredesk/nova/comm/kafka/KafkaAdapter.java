/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.kafka;

import ch.squaredesk.nova.comm.CommAdapterBuilder;
import ch.squaredesk.nova.comm.retrieving.MessageReceiver;
import ch.squaredesk.nova.comm.sending.MessageSender;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class KafkaAdapter<InternalMessageType> {
    private static final Logger logger = LoggerFactory.getLogger(KafkaAdapter.class);

    private final MessageSender<InternalMessageType, OutgoingMessageMetaData> messageSender;
    private final MessageReceiver<String, InternalMessageType, IncomingMessageMetaData> messageReceiver;


    KafkaAdapter(MessageSender<InternalMessageType, OutgoingMessageMetaData> messageSender,
                 MessageReceiver<String, InternalMessageType, IncomingMessageMetaData> messageReceiver) {
        this.messageReceiver = messageReceiver;
        this.messageSender = messageSender;
    }

    /////////////////////////////////
    //                             //
    // simple send related methods //
    //                             //
    /////////////////////////////////
    public <ConcreteMessageType extends InternalMessageType> Completable sendMessage(
            String destination, ConcreteMessageType message) {
        requireNonNull(message, "message must not be null");
        SendInfo sendInfo = new SendInfo();
        OutgoingMessageMetaData meta = new OutgoingMessageMetaData(destination, sendInfo);
        return this.messageSender.doSend(message, meta)
        /*.doOnError(t -> examineSendExceptionForDeadDestinationAndInformListener(t, destination))*/;
    }

    //////////////////////////////////
    //                              //
    // subscription related methods //
    //                              //
    //////////////////////////////////
    public Flowable<InternalMessageType> messages (String destination) {
        return messageReceiver.messages(destination).map(incomingMessage -> incomingMessage.message);
    }


    /////////////////////////
    //                     //
    //  lifecycle  methods //
    //                     //
    /////////////////////////
    public void shutdown() {
        if (messageReceiver instanceof KafkaMessageReceiver) {
            ((KafkaMessageReceiver)messageReceiver).shutdown();
        }
        logger.info("KafkaAdapter shut down");
    }


    /////////////////////////
    //                     //
    //    the  Builder     //
    //                     //
    /////////////////////////
    public static <InternalMessageType> Builder<InternalMessageType> builder(Class<InternalMessageType> messageTypeClass) {
        return new Builder<>(messageTypeClass);
    }

    public static class Builder<InternalMessageType> extends CommAdapterBuilder<InternalMessageType, KafkaAdapter<InternalMessageType>>{
        private String serverAddress;
        private String identifier;
        private MessageSender<InternalMessageType, OutgoingMessageMetaData> messageSender;
        private MessageReceiver<String, InternalMessageType, IncomingMessageMetaData> messageReceiver;
        private Scheduler subscriptionScheduler;
        private Properties consumerProperties = new Properties();
        private Properties producerProperties = new Properties();
        private long pollTimeout = 1;
        private TimeUnit pollTimeUnit = TimeUnit.SECONDS;

        private Builder(Class<InternalMessageType> messageTypeClass) {
            super(messageTypeClass);
        }

        public Builder<InternalMessageType> setMessagePollingTimeout(long pollTimeout, TimeUnit pollTimeUnit) {
            requireNonNull(pollTimeUnit, "pollTimeUnit must not be null");
            this.pollTimeout = pollTimeout;
            this.pollTimeUnit = pollTimeUnit;
            return this;
        }

        public Builder<InternalMessageType> setConsumerProperties(Properties consumerProperties) {
            if (consumerProperties!=null) {
                this.consumerProperties.putAll(consumerProperties);
            }
            return this;
        }

        private Builder<InternalMessageType> addProperty(Properties target, String key, String value) {
            requireNonNull(key, "property key must not be null");
            requireNonNull(value, "value for property " + key + " must not be null");
            target.setProperty(key, value);
            return this;
        }

        public Builder<InternalMessageType> addConsumerProperty(String key, String value) {
            return addProperty(consumerProperties, key, value);
        }

        public Builder<InternalMessageType> addProducerProperty(String key, String value) {
            return addProperty(producerProperties, key, value);
        }

        public Builder<InternalMessageType> setProducerProperties(Properties producerProperties) {
            if (producerProperties != null) {
                this.producerProperties.putAll(producerProperties);
            }
            return this;
        }

        public Builder<InternalMessageType> setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder<InternalMessageType> setSubscriptionScheduler(Scheduler scheduler) {
            this.subscriptionScheduler = scheduler;
            return this;
        }

        public Builder<InternalMessageType> setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder<InternalMessageType> setMessageSender(MessageSender<InternalMessageType, OutgoingMessageMetaData> messageSender) {
            this.messageSender = messageSender;
            return this;
        }

        public Builder<InternalMessageType> setMessageReceiver(MessageReceiver<String, InternalMessageType, IncomingMessageMetaData> messageReceiver) {
            this.messageReceiver = messageReceiver;
            return this;
        }

        public void validate() {
            requireNonNull(serverAddress,"serverAddress must be provided");
            requireNonNull(messageUnmarshaller,"messageUnmarshaller must be provided");
            requireNonNull(messageMarshaller,"messageMarshaller must be provided");
            requireNonNull(metrics,"metrics must be provided");
            if (subscriptionScheduler==null) {
                subscriptionScheduler = Schedulers.from(Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "KafkaSubscription");
                    t.setDaemon(true);
                    return t;
                }));
            }
            if (consumerProperties==null) consumerProperties = new Properties();
            if (producerProperties==null) producerProperties = new Properties();
        }

        public KafkaAdapter<InternalMessageType> createInstance() {
            // set a few default consumer and producer properties
            String clientId = identifier == null ? "KafkaAdapter-"+UUID.randomUUID() : identifier;
            String groupId = identifier == null ? "KafkaAdapter-ReadGroup" : identifier + "ReadGroup";
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
                messageReceiver = new KafkaMessageReceiver<>(identifier, consumerProperties, messageUnmarshaller, pollTimeout, pollTimeUnit, metrics);
            }
            if (messageSender == null) {
                messageSender = new KafkaMessageSender<>(identifier, producerProperties, messageMarshaller, metrics);
            }
            return new KafkaAdapter<>(this.messageSender, this.messageReceiver);
        }

        private static void setPropertyIfNotPresent (Properties props, String key, String value) {
            if (!props.containsKey(key)) {
                props.setProperty(key, value);
            }
        }
    }
}
