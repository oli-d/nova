/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import ch.squaredesk.nova.comm.CommAdapterBuilder;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class JmsAdapter<InternalMessageType> {
    private static final Logger logger = LoggerFactory.getLogger(JmsAdapter.class);

    private final ch.squaredesk.nova.comm.sending.MessageSender<InternalMessageType, OutgoingMessageMetaData> messageSender;
    private final ch.squaredesk.nova.comm.retrieving.MessageReceiver<Destination, InternalMessageType, IncomingMessageMetaData> messageReceiver;
    private final RpcClient<InternalMessageType> rpcClient;
    private final RpcServer<InternalMessageType> rpcServer;
    private final JmsObjectRepository jmsObjectRepository;
    private final int defaultMessagePriority;
    private final long defaultMessageTimeToLive;
    private final int defaultMessageDeliveryMode;
    private final ConcurrentLinkedDeque<Consumer<Destination>> destinationListeners = new ConcurrentLinkedDeque<>();
    private final Supplier<String> correlationIdGenerator;
    private final long defaultRpcTimeout;
    private final TimeUnit defaultRpcTimeUnit;


    JmsAdapter(Builder<InternalMessageType> builder) {
        this.messageReceiver = builder.messageReceiver;
        this.messageSender = builder.messageSender;
        this.rpcServer = builder.rpcServer;
        this.rpcClient = builder.rpcClient;
        this.correlationIdGenerator = builder.correlationIdGenerator;
        this.jmsObjectRepository = builder.jmsObjectRepository;
        this.defaultMessageDeliveryMode = builder.defaultDeliveryMode;
        this.defaultMessagePriority = builder.defaultPriority;
        this.defaultMessageTimeToLive = builder.defaultTimeToLive;
        this.defaultRpcTimeout = builder.defaultRpcTimeout;
        this.defaultRpcTimeUnit = builder.defaultRpcTimeUnit;
    }

    /////////////////////////////////
    //                             //
    // simple send related methods //
    //                             //
    /////////////////////////////////
    public <ConcreteMessageType extends InternalMessageType> Completable sendMessage(
            Destination destination, ConcreteMessageType message) {
        return doSendMessage(destination, message, null, null, null, null);
    }

    public <ConcreteMessageType extends InternalMessageType> Completable sendMessage(
            Destination destination,
            ConcreteMessageType message,
            Map<String, Object> customHeaders) {
        return doSendMessage(destination, message, customHeaders, null, null, null);
    }

    protected <ConcreteMessageType extends InternalMessageType> Completable doSendMessage(
            Destination destination,
            ConcreteMessageType message,
            Map<String, Object> customHeaders,
            Integer deliveryMode,
            Integer priority,
            Long timeToLive) {

        requireNonNull(message, "message must not be null");
        SendInfo jmsSpecificSendingInfo = new SendInfo(
                null,
                null,
                customHeaders,
                deliveryMode == null ? defaultMessageDeliveryMode : deliveryMode,
                priority == null ? defaultMessagePriority : priority,
                timeToLive == null ? defaultMessageTimeToLive : timeToLive);
        OutgoingMessageMetaData meta = new OutgoingMessageMetaData(destination, jmsSpecificSendingInfo);
        return this.messageSender.doSend(message, meta)
                .doOnError(t -> examineSendExceptionForDeadDestinationAndInformListener(t, destination));
    }

    //////////////////////////////////
    //                              //
    // subscription related methods //
    //                              //
    //////////////////////////////////
    public Flowable<InternalMessageType> messages (Destination destination) {
        return this.messageReceiver.messages(destination)
                .filter(incomingMessage -> !incomingMessage.metaData.details.isRpcReply())
                .map(incomingMessage -> incomingMessage.message);
    }

    ////////////////////////
    //                    //
    // RPC server methods //
    //                    //
    ////////////////////////
    public Flowable<RpcInvocation<InternalMessageType>> requests(Destination destination) {
        requireNonNull(destination, "destination must not be null");
        return rpcServer.requests(destination);
    }

    ////////////////////////
    //                    //
    // RPC client methods //
    //                    //
    ////////////////////////
    private <ReplyType extends InternalMessageType> Single<RpcReply<ReplyType>> sendRequest(
            Destination destination,
            Destination replyDestination,
            InternalMessageType request,
            Map<String, Object> customHeaders,
            Integer deliveryMode, Integer priority, Long timeToLive,
            Long timeout, TimeUnit timeUnit) {

        // prepare message sending info
        requireNonNull(replyDestination, "ReplyDestination must not be null");
        if (timeout != null) {
            requireNonNull(timeUnit, "timeUnit must not be null if timeout specified");
        } else {
            timeout = defaultRpcTimeout;
            timeUnit = defaultRpcTimeUnit;
        }
        int deliveryModeToUse = deliveryMode == null ? defaultMessageDeliveryMode : deliveryMode;
        int priorityToUse = priority == null ? defaultMessagePriority : priority;
        long timeToLiveToUse = timeToLive == null ? defaultMessageTimeToLive : timeToLive;
        // it doesn't make sense to let request messages live longer than timeout:
        timeToLiveToUse = Math.min(timeToLiveToUse, timeUnit.toMillis(timeout));
        String correlationId = correlationIdGenerator.get();
        SendInfo jmsSpecificInfo = new SendInfo(
                correlationId, replyDestination, customHeaders, deliveryModeToUse, priorityToUse, timeToLiveToUse);
        OutgoingMessageMetaData sendingInfo = new OutgoingMessageMetaData(destination, jmsSpecificInfo);

        return rpcClient.<ReplyType>sendRequest(request, sendingInfo, timeout, timeUnit)
                .doOnError(t -> examineSendExceptionForDeadDestinationAndInformListener(t, destination));
    }

    public <ReplyType extends InternalMessageType> Single<RpcReply<ReplyType>> sendRequest(
            Destination destination,
            Destination replyDestination,
            InternalMessageType message,
            Map<String, Object> customHeaders,
            long timeout, TimeUnit timeUnit) {

        return sendRequest(destination, replyDestination, message, customHeaders, null, null, null,timeout, timeUnit);
    }

    public <ReplyType extends InternalMessageType> Single<RpcReply<ReplyType>> sendRequest(
            Destination destination,
            InternalMessageType message,
            Map<String, Object> customHeaders,
            Long timeout, TimeUnit timeUnit) {

        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, customHeaders, null, null, null, timeout, timeUnit);
    }

    public <ReplyType extends InternalMessageType> Single<RpcReply<ReplyType>> sendRequest(
            Destination destination,
            InternalMessageType message,
            Map<String, Object> customHeaders) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, customHeaders, null, null, null, null, null);
    }

    public <ReplyType extends InternalMessageType> Single<RpcReply<ReplyType>> sendRequest(
            Destination destination,
            InternalMessageType message,
            long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, null, null, null, null, timeout, timeUnit);
    }
    public <ReplyType extends InternalMessageType> Single<RpcReply<ReplyType>> sendRequest(
            Destination destination,
            InternalMessageType message) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, null, null, null, null, null, null);
    }

    /////////////////////////////////////////
    //                                     //
    // DestinationListener related methods //
    //                                     //
    /////////////////////////////////////////
    public void addDestinationListener(Consumer<Destination> destinationListener) {
        this.destinationListeners.add(destinationListener);
    }

    public void removeDestinationListener(Consumer<Destination> destinationListener) {
        this.destinationListeners.remove(destinationListener);
    }

    private void notifyDestinationListenersAboutDeadDestination(Destination deadDestination) {
        this.destinationListeners.forEach(consumer -> {
            try {
                consumer.accept(deadDestination);
            } catch (Exception e) {
                logger.error("An error occurred trying to inform listener about dead destination {}", deadDestination, e);
            }
        });
    }

    private void examineSendExceptionForDeadDestinationAndInformListener(Throwable error, Destination destination) {
        if (exceptionSignalsDestinationDown(error)) {
            notifyDestinationListenersAboutDeadDestination(destination);
        }
    }

    private static boolean exceptionSignalsDestinationDown(Throwable errorToExamine) {
        // TODO: is there a proper way to determine this???!?!?!?! Works for ActiveMQ, but how do other brokers behave?
        Function<Throwable, Boolean> testFunc = ex ->
                (ex instanceof InvalidDestinationException) ||
                        (String.valueOf(ex).contains("does not exist"));

        Throwable error = errorToExamine;
        boolean down = testFunc.apply(error);
        while (!down && error != null && error.getCause() != null && error.getCause() != error) {
            error = error.getCause();
            down = testFunc.apply(error);
        }

        return down;
    }


    /////////////////////////
    //                     //
    //  lifecycle  methods //
    //                     //
    /////////////////////////
    public void shutdown() {
        jmsObjectRepository.shutdown();
    }

    public void start() throws JMSException {
        jmsObjectRepository.start();
    }

    /////////////////////////
    //                     //
    //    the  Builder     //
    //                     //
    /////////////////////////
    public static <InternalMessageType> Builder<InternalMessageType> builder(Class<InternalMessageType> messageTypeClass) {
        return new Builder<>(messageTypeClass);
    }

    public static class Builder<InternalMessageType> extends CommAdapterBuilder<InternalMessageType, JmsAdapter<InternalMessageType>>{
        private String identifier;
        private Supplier<String> correlationIdGenerator;
        private Function<Throwable, InternalMessageType> errorReplyFactory;
        private Function<Destination, String> destinationIdGenerator;
        private ConnectionFactory connectionFactory;
        private boolean consumerSessionTransacted = false;
        private int consumerSessionAckMode = Session.AUTO_ACKNOWLEDGE;
        private boolean producerSessionTransacted = false;
        private int producerSessionAckMode = Session.AUTO_ACKNOWLEDGE;
        private int defaultPriority = Message.DEFAULT_PRIORITY;
        private long defaultTimeToLive = Message.DEFAULT_TIME_TO_LIVE;
        private int defaultDeliveryMode = DeliveryMode.NON_PERSISTENT;
        private JmsObjectRepository jmsObjectRepository;
        private ch.squaredesk.nova.comm.sending.MessageSender<InternalMessageType, OutgoingMessageMetaData> messageSender;
        private ch.squaredesk.nova.comm.retrieving.MessageReceiver<Destination, InternalMessageType, IncomingMessageMetaData> messageReceiver;
        private RpcServer<InternalMessageType> rpcServer;
        private RpcClient<InternalMessageType> rpcClient;
        private long defaultRpcTimeout;
        private TimeUnit defaultRpcTimeUnit;

        private Builder(Class<InternalMessageType> messageTypeClass) {
            super(messageTypeClass);
        }

        public Builder<InternalMessageType> setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder<InternalMessageType> setDefaultRpcTimeout(long defaultRpcTimeout, TimeUnit timeUnit) {
            this.defaultRpcTimeout = defaultRpcTimeout;
            this.defaultRpcTimeUnit = timeUnit;
            return this;
        }

        public Builder<InternalMessageType> setDefaultMessagePriority(int priority) {
            this.defaultPriority = priority;
            return this;
        }

        public Builder<InternalMessageType> setDefaultMessageTimeToLive(long timeToLive) {
            this.defaultTimeToLive = timeToLive;
            return this;
        }

        public Builder<InternalMessageType> setDefaultMessageDeliveryMode(int deliveryMode) {
            this.defaultDeliveryMode = deliveryMode;
            return this;
        }

        public Builder<InternalMessageType> setConnectionFactory(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        public Builder<InternalMessageType> setProducerSessionTransacted(boolean sessionTransacted) {
            this.producerSessionTransacted = sessionTransacted;
            return this;
        }

        public Builder<InternalMessageType> setProducerSessionAckMode(int sessionAckMode) {
            this.producerSessionAckMode = sessionAckMode;
            return this;
        }

        public Builder<InternalMessageType> setConsumerSessionTransacted(boolean sessionTransacted) {
            this.consumerSessionTransacted = sessionTransacted;
            return this;
        }

        public Builder<InternalMessageType> setConsumerSessionAckMode(int sessionAckMode) {
            this.consumerSessionAckMode = sessionAckMode;
            return this;
        }

        public Builder<InternalMessageType> setCorrelationIdGenerator(Supplier<String> correlationIdGenerator) {
            this.correlationIdGenerator = correlationIdGenerator;
            return this;
        }

        public Builder<InternalMessageType> setDestinationIdGenerator(Function<Destination, String> destinationIdGenerator) {
            this.destinationIdGenerator = destinationIdGenerator;
            return this;
        }

        public Builder<InternalMessageType> setErrorReplyFactory(Function<Throwable, InternalMessageType> errorReplyFactory) {
            this.errorReplyFactory = errorReplyFactory;
            return this;
        }


        // for testing
        public Builder<InternalMessageType> setJmsObjectRepository(JmsObjectRepository jmsObjectRepository) {
            this.jmsObjectRepository = jmsObjectRepository;
            return this;
        }

        public Builder<InternalMessageType> setRpcClient(RpcClient<InternalMessageType> rpcClient) {
            this.rpcClient = rpcClient;
            return this;
        }

        public Builder<InternalMessageType> setRpcServer(RpcServer<InternalMessageType> rpcServer) {
            this.rpcServer = rpcServer;
            return this;
        }

        public Builder<InternalMessageType> setMessageReceiver(ch.squaredesk.nova.comm.retrieving.MessageReceiver<Destination, InternalMessageType, IncomingMessageMetaData> messageReceiver) {
            this.messageReceiver = messageReceiver;
            return this;
        }

        public Builder<InternalMessageType> setMessageSender(ch.squaredesk.nova.comm.sending.MessageSender<InternalMessageType, OutgoingMessageMetaData> messageSender) {
            this.messageSender = messageSender;
            return this;
        }

        protected void validate() {
            requireNonNull(metrics,"metrics must be provided");
            requireNonNull(messageUnmarshaller,"messageUnmarshaller must be provided");
            requireNonNull(messageMarshaller,"messageMarshaller must be provided");
            requireNonNull(errorReplyFactory,"errorReplyFactory must be provided");

            if (messageSender == null || messageReceiver == null) {
                requireNonNull(connectionFactory, "connectionFactory must be provided");
            }

            if (correlationIdGenerator == null) {
                correlationIdGenerator = new UIDCorrelationIdGenerator();
            }
            if (destinationIdGenerator == null) {
                destinationIdGenerator = new DefaultDestinationIdGenerator();
            }

            if (defaultRpcTimeout <= 0 || defaultRpcTimeUnit==null) {
                defaultRpcTimeout = 30;
                defaultRpcTimeUnit = TimeUnit.SECONDS;
            }
        }

        public JmsAdapter<InternalMessageType> createInstance() {
            if (messageSender==null || messageReceiver == null) {
                Connection connection;
                try {
                    logger.debug("Creating connection to broker...");
                    connection = connectionFactory.createConnection();
                } catch (JMSException e) {
                    throw new RuntimeException("Unable to create JMS session", e);
                }
                logger.debug("Creating JmsObjectRepository...");
                JmsSessionDescriptor producerSessionDescriptor = new JmsSessionDescriptor(producerSessionTransacted, producerSessionAckMode);
                JmsSessionDescriptor consumerSessionDescriptor = new JmsSessionDescriptor(consumerSessionTransacted, consumerSessionAckMode);
                jmsObjectRepository = new JmsObjectRepository(connection, producerSessionDescriptor, consumerSessionDescriptor, destinationIdGenerator);
            }

            if (messageSender == null) {
                messageSender = new MessageSender<>(identifier, jmsObjectRepository, messageMarshaller, metrics);
            }
            if (messageReceiver == null) {
                messageReceiver = new MessageReceiver<>(identifier, jmsObjectRepository, messageUnmarshaller, metrics);
            }
            if (rpcServer == null) {
                rpcServer = new RpcServer<>(identifier, messageReceiver, messageSender, errorReplyFactory, metrics);
            }
            if (rpcClient==null) {
                rpcClient = new RpcClient<>(identifier, messageSender, messageReceiver, metrics);
            }
            return new JmsAdapter<>(this);
        }
    }
}
