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

import ch.squaredesk.nova.comm.CommAdapter;
import ch.squaredesk.nova.comm.CommAdapterBuilder;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
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
    public Completable sendMessage(String destination, String message) {
        SendInfo sendInfo = new SendInfo();
        OutgoingMessageMetaData meta = new OutgoingMessageMetaData(destination, sendInfo);
        return messageSender.send(message, meta);
    }

    public <T> Completable sendMessage(String destination, T message) {
        Function<T, String> transcriber = messageTranscriber.getOutgoingMessageTranscriber((Class<T>)message.getClass());
        return sendMessage(destination, message, transcriber);
    }

    public <T> Completable sendMessage(String destination, T message, Function<T, String> transcriber) {
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
        if (messageReceiver instanceof MessageReceiver) {
            ((MessageReceiver)messageReceiver).shutdown();
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
        private String identifier;
        private MessageSender messageSender;
        private MessageReceiver messageReceiver;
        private Scheduler subscriptionScheduler;
        private Properties consumerProperties = new Properties();
        private Properties producerProperties = new Properties();
        private long pollTimeout = 1;
        private TimeUnit pollTimeUnit = TimeUnit.SECONDS;

        private Builder() {
        }

        public Builder setMessagePollingTimeout(long pollTimeout, TimeUnit pollTimeUnit) {
            requireNonNull(pollTimeUnit, "pollTimeUnit must not be null");
            this.pollTimeout = pollTimeout;
            this.pollTimeUnit = pollTimeUnit;
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

        public Builder setSubscriptionScheduler(Scheduler scheduler) {
            this.subscriptionScheduler = scheduler;
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

        public void validate() {
            requireNonNull(serverAddress,"serverAddress must be provided");
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

        public KafkaAdapter createInstance() {
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
                messageReceiver = new MessageReceiver(identifier, consumerProperties, pollTimeout, pollTimeUnit, metrics);
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
            if (!props.containsKey(key)) {
                props.setProperty(key, value);
            }
        }
    }
}
