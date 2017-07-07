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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

public class KafkaCommAdapter<InternalMessageType> {
    private static final Logger logger = LoggerFactory.getLogger(KafkaCommAdapter.class);

    private final KafkaMessageSender<InternalMessageType> messageSender;
    private final KafkaMessageReceiver<InternalMessageType> messageReceiver;
    private final BackpressureStrategy defaultBackpressureStrategy;
    private final KafkaObjectFactory kafkaObjectFactory;


    protected KafkaCommAdapter(Builder<InternalMessageType> builder) {
        this.messageReceiver = builder.messageReceiver;
        this.messageSender = builder.messageSender;
        this.defaultBackpressureStrategy = builder.defaultBackpressureStrategy;
        this.kafkaObjectFactory = builder.kafkaObjectFactory;
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
        private String identifier;
        private Metrics metrics;
        private MessageUnmarshaller<String,InternalMessageType> messageUnmarshaller;
        private MessageMarshaller<InternalMessageType,String> messageMarshaller;
        private BackpressureStrategy defaultBackpressureStrategy = BackpressureStrategy.BUFFER;
        private KafkaMessageSender<InternalMessageType> messageSender;
        private KafkaMessageReceiver<InternalMessageType> messageReceiver;
        private KafkaObjectFactory kafkaObjectFactory;
        private Scheduler subscriptionScheduler;

        private Builder() {
        }

        public Builder<InternalMessageType> setSubscriptionScheduler(Scheduler scheduler) {
            this.subscriptionScheduler = scheduler;
            return this;
        }

        public Builder<InternalMessageType> setKafkaObjectFactory(KafkaObjectFactory kafkaObjectFactory) {
            this.kafkaObjectFactory = kafkaObjectFactory;
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
            requireNonNull(kafkaObjectFactory,"kafkaObjectFactory must be provided");
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
            return this;
        }

        public KafkaCommAdapter<InternalMessageType> build() {
            validate();
            messageReceiver = new KafkaMessageReceiver<>(identifier, kafkaObjectFactory, subscriptionScheduler, messageUnmarshaller, metrics);
            messageSender = new KafkaMessageSender<>(identifier, kafkaObjectFactory, messageMarshaller, metrics);
            return new KafkaCommAdapter<>(this);
        }
    }
}
