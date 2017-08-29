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

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
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

import static java.util.Objects.requireNonNull;

public class KafkaCommAdapter<InternalMessageType> {
    private static final Logger logger = LoggerFactory.getLogger(KafkaCommAdapter.class);

    private final KafkaMessageSender<InternalMessageType> messageSender;
    private final KafkaMessageReceiver<InternalMessageType> messageReceiver;
    private final BackpressureStrategy defaultBackpressureStrategy;
    private final KafkaObjectFactory kafkaObjectFactory;


    KafkaCommAdapter(KafkaMessageSender<InternalMessageType> messageSender,
                     KafkaMessageReceiver<InternalMessageType> messageReceiver,
                     BackpressureStrategy defaultBackpressureStrategy,
                     KafkaObjectFactory kafkaObjectFactory) {
        this.messageReceiver = messageReceiver;
        this.messageSender = messageSender;
        this.defaultBackpressureStrategy = defaultBackpressureStrategy;
        this.kafkaObjectFactory = kafkaObjectFactory;
    }

    /////////////////////////////////
    //                             //
    // simple send related methods //
    //                             //
    /////////////////////////////////
    public <ConcreteMessageType extends InternalMessageType> Completable sendMessage(
            String destination, ConcreteMessageType message) {
        requireNonNull(message, "message must not be null");
        KafkaSpecificInfo jmsSpecificSendingInfo = new KafkaSpecificInfo();

        return this.messageSender.sendMessage(
                destination,
                message,
                jmsSpecificSendingInfo
        )/*.doOnError(t -> examineSendExceptionForDeadDestinationAndInformListener(t, destination))*/;
    }

    //////////////////////////////////
    //                              //
    // subscription related methods //
    //                              //
    //////////////////////////////////
    public Flowable<InternalMessageType> messages (String destination) {
        return messages(destination, defaultBackpressureStrategy);
    }

    public Flowable<InternalMessageType> messages (
            String destination, BackpressureStrategy backpressureStrategy) {
        return this.messageReceiver.messages(destination, backpressureStrategy)
                .map(incomingMessage -> incomingMessage.message);
    }

    /////////////////////////
    //                     //
    //  lifecycle  methods //
    //                     //
    /////////////////////////
    public void shutdown() {
        kafkaObjectFactory.shutdown();
        logger.info("KafkaCommAdapter shut down");
    }


    /////////////////////////
    //                     //
    //    the  Builder     //
    //                     //
    /////////////////////////
    public static <InternalMessageType> Builder<InternalMessageType> builder() {
        return new Builder<>();
    }

    public static class Builder<InternalMessageType> {
        private String serverAddress;
        private String identifier;
        private Metrics metrics;
        private MessageUnmarshaller<String,InternalMessageType> messageUnmarshaller;
        private MessageMarshaller<InternalMessageType,String> messageMarshaller;
        private BackpressureStrategy defaultBackpressureStrategy = BackpressureStrategy.BUFFER;
        private KafkaMessageSender<InternalMessageType> messageSender;
        private KafkaMessageReceiver<InternalMessageType> messageReceiver;
        private KafkaObjectFactory kafkaObjectFactory;
        private Scheduler subscriptionScheduler;
        private Properties consumerProperties;
        private Properties producerProperties;

        private Builder() {
        }

        public Builder<InternalMessageType> setConsumerProperties(Properties consumerProperties) {
            this.consumerProperties = new Properties();
            if (consumerProperties!=null) {
                this.consumerProperties.putAll(consumerProperties);
            }
            return this;
        }

        public Builder<InternalMessageType> setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder<InternalMessageType> setProducerProperties(Properties producerProperties) {
            this.producerProperties = new Properties();
            if (producerProperties != null) {
                this.producerProperties.putAll(producerProperties);
            }
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

        public Builder<InternalMessageType> setMetrics(Metrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder<InternalMessageType> setDefaultBackpressureStrategy(BackpressureStrategy defaultBackpressureStrategy) {
            this.defaultBackpressureStrategy = defaultBackpressureStrategy;
            return this;
        }

        public Builder<InternalMessageType> setMessageMarshaller(MessageMarshaller<InternalMessageType, String> messageMarshaller) {
            this.messageMarshaller = messageMarshaller;
            return this;
        }

        public Builder<InternalMessageType> setMessageUnmarshaller(MessageUnmarshaller<String,InternalMessageType> messageUnmarshaller) {
            this.messageUnmarshaller = messageUnmarshaller;
            return this;
        }

        public Builder<InternalMessageType> validate() {
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
            return this;
        }

        public KafkaCommAdapter<InternalMessageType> build() {
            validate();

            // set a few default consumer and producer properties
            String clientId = identifier == null ? "KafkaCommAdapter-"+UUID.randomUUID() : identifier;
            String groupId = identifier == null ? "KafkaCommAdapter-ReadGroup" : identifier + "ReadGroup";
            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddress);
            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.CLIENT_ID_CONFIG, clientId);
            setPropertyIfNotPresent(consumerProperties, ConsumerConfig.GROUP_ID_CONFIG, groupId);

            setPropertyIfNotPresent(producerProperties, ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddress);
            setPropertyIfNotPresent(producerProperties, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            setPropertyIfNotPresent(producerProperties, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            setPropertyIfNotPresent(producerProperties, ProducerConfig.CLIENT_ID_CONFIG, clientId);

            kafkaObjectFactory = new KafkaObjectFactory(this.consumerProperties, this.producerProperties);
            messageReceiver = new KafkaMessageReceiver<>(identifier, kafkaObjectFactory, subscriptionScheduler, messageUnmarshaller, metrics);
            messageSender = new KafkaMessageSender<>(identifier, kafkaObjectFactory, messageMarshaller, metrics);
            return new KafkaCommAdapter<>(this.messageSender,
                    this.messageReceiver,
                    this.defaultBackpressureStrategy,
                    this.kafkaObjectFactory);
        }

        private static void setPropertyIfNotPresent (Properties props, String key, String value) {
            if (!props.containsKey(key)) {
                props.setProperty(key, value);
            }
        }
    }
}
