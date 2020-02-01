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

import ch.squaredesk.nova.comm.CommAdapter;
import ch.squaredesk.nova.comm.CommAdapterBuilder;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class JmsAdapter extends CommAdapter<String> {
    private static final Logger logger = LoggerFactory.getLogger(JmsAdapter.class);

    private final MessageSender messageSender;
    private final MessageReceiver messageReceiver;
    private final RpcClient rpcClient;
    private final RpcServer rpcServer;
    private final JmsObjectRepository jmsObjectRepository;
    private final int defaultMessagePriority;
    private final long defaultMessageTimeToLive;
    private final int defaultMessageDeliveryMode;
    private final ConcurrentLinkedDeque<Consumer<Destination>> destinationListeners = new ConcurrentLinkedDeque<>();
    private final Supplier<String> correlationIdGenerator;
    private final Duration defaultRpcTimeout;


    JmsAdapter(Builder builder) {
        super(builder.messageTranscriber, builder.metrics);
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
    }

    /////////////////////////////////
    //                             //
    // simple send related methods //
    //                             //
    /////////////////////////////////
    public <T> Single<OutgoingMessageMetaData> sendMessage(Destination destination, T message) throws Exception {
        return sendMessage(destination, message, null, null, null, null);
    }

    public <T> Single<OutgoingMessageMetaData> sendMessage(Destination destination, T message, Map<String, Object> customHeaders) {
        return sendMessage(destination, message, customHeaders, null, null, null);
    }

    public <T> Single<OutgoingMessageMetaData> sendMessage(Destination destination, T message, Map<String, Object> customHeaders, Integer deliveryMode, Integer priority, Long timeToLive) {
        Function<T, String> transcriber = messageTranscriber.getOutgoingMessageTranscriber((Class<T>) message.getClass());
        return doSendMessage(destination, message, transcriber, customHeaders, deliveryMode, priority, timeToLive);
    }

    public <T> Single<OutgoingMessageMetaData> sendMessage(Destination destination, T message, Function<T, String> transcriber) throws Exception {
        return doSendMessage(destination, message, transcriber, null, null, null, null);
    }

    public <T> Single<OutgoingMessageMetaData> sendMessage(Destination destination, T message, Function<T, String> transcriber, Map<String, Object> customHeaders) {
        return doSendMessage(destination, message, transcriber, customHeaders, null, null, null);
    }

    public <T> Single<OutgoingMessageMetaData> sendMessage(Destination destination, T message, Function<T, String> transcriber, Map<String, Object> customHeaders, Integer deliveryMode, Integer priority, Long timeToLive) {
        return doSendMessage(destination, message, transcriber, customHeaders, deliveryMode, priority, timeToLive);
    }

    protected <T> Single<OutgoingMessageMetaData> doSendMessage(
            Destination destination,
            T message,
            Function<T,String> transcriber,
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
        return this.messageSender.send(message, meta, transcriber)
                .doOnError(t -> examineSendExceptionForDeadDestinationAndInformListener(t, destination));
    }

    public Single<OutgoingMessageMetaData> sendMessage(Destination destination, String message) {
        return doSendMessage(destination, message, null, null, null, null);
    }

    public Single<OutgoingMessageMetaData> sendMessage(Destination destination, String message, Map<String, Object> customHeaders) {
        return doSendMessage(destination, message, customHeaders, null, null, null);
    }

    protected Single<OutgoingMessageMetaData> doSendMessage(
            Destination destination,
            String message,
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
        return this.messageSender.send(message, meta)
                .doOnError(t -> examineSendExceptionForDeadDestinationAndInformListener(t, destination));
    }

    //////////////////////////////////
    //                              //
    // subscription related methods //
    //                              //
    //////////////////////////////////
    public Flowable<String> messages (Destination destination) {
        return this.messageReceiver.messages(destination)
                .filter(incomingMessage -> !incomingMessage.metaData.details.isRpcReply())
                .map(incomingMessage -> incomingMessage.message);
    }

    public <T> Flowable<T> messages(Destination destination, Class<T> messageType) {
        return messages(destination, messageTranscriber.getIncomingMessageTranscriber(messageType));
    }

    public <T> Flowable<T> messages(Destination destination, Function<String, T> messageTranscriber) {
        return messageReceiver.messages(destination, messageTranscriber)
                .map(incomingMessage -> incomingMessage.message);
    }

    ////////////////////////
    //                    //
    // RPC server methods //
    //                    //
    ////////////////////////
    public <T> Flowable<RpcInvocation<T>> requests(Destination destination, Class<T> requestType) {
        requireNonNull(destination, "destination must not be null");
        return rpcServer.requests(destination, requestType);
    }

    ////////////////////////
    //                    //
    // RPC client methods //
    //                    //
    ////////////////////////
    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            Destination replyDestination,
            T request,
            Map<String, Object> customHeaders,
            Function<String, U> replyTranscriber,
            Integer deliveryMode, Integer priority, Long timeToLive,
            Duration timeout) {

        // prepare message sending info
        requireNonNull(replyDestination, "ReplyDestination must not be null");
        Duration timeoutToUse = Optional.ofNullable(timeout).orElse(defaultRpcTimeout);
        int deliveryModeToUse = deliveryMode == null ? defaultMessageDeliveryMode : deliveryMode;
        int priorityToUse = priority == null ? defaultMessagePriority : priority;
        long timeToLiveToUse = timeToLive == null ? defaultMessageTimeToLive : timeToLive;
        // it doesn't make sense to let request messages live longer than timeout:
        timeToLiveToUse = Math.min(timeToLiveToUse, timeoutToUse.toMillis());
        String correlationId = correlationIdGenerator.get();
        SendInfo jmsSpecificInfo = new SendInfo(
                correlationId, replyDestination, customHeaders, deliveryModeToUse, priorityToUse, timeToLiveToUse);
        OutgoingMessageMetaData sendingInfo = new OutgoingMessageMetaData(destination, jmsSpecificInfo);

        return rpcClient.sendRequest(
                request,
                sendingInfo,
                messageTranscriber.getOutgoingMessageTranscriber(request),
                replyTranscriber,
                timeoutToUse)
                .doOnError(t -> examineSendExceptionForDeadDestinationAndInformListener(t, destination));
    }

    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            Destination replyDestination,
            T message,
            Map<String, Object> customHeaders,
            Function<String, U> replyTranscriber,
            Duration timeout) {
        return sendRequest(destination, replyDestination, message, customHeaders, replyTranscriber, null, null, null, timeout);
    }

    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            T message,
            Map<String, Object> customHeaders,
            Function<String, U> replyTranscriber,
            Duration timeout) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, customHeaders, replyTranscriber, null, null, null, timeout);
    }

    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            T message,
            Map<String, Object> customHeaders,
            Function<String, U> replyTranscriber) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, customHeaders, replyTranscriber, null, null, null, null);
    }

    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            T message,
            Function<String, U> replyTranscriber,
            Duration timeout) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, null, replyTranscriber, null, null, null, timeout);
    }
    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            T message,
            Function<String, U> replyTranscriber) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, null, replyTranscriber, null, null, null, null);
    }

    public <T, U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            Destination replyDestination,
            T request,
            Map<String, Object> customHeaders,
            Class<U> replyType,
            Integer deliveryMode, Integer priority, Long timeToLive,
            Duration timeout) {
        return sendRequest(destination, replyDestination, request, customHeaders, messageTranscriber.getIncomingMessageTranscriber(replyType), deliveryMode, priority, timeToLive, timeout);
    }
        public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            Destination replyDestination,
            T message,
            Map<String, Object> customHeaders,
            Class<U> replyType,
            Duration timeout) {
            return sendRequest(destination, replyDestination, message, customHeaders, messageTranscriber.getIncomingMessageTranscriber(replyType), null, null, null,timeout);
    }

    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            T message,
            Map<String, Object> customHeaders,
            Class<U> replyType,
            Duration timeout) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, customHeaders, messageTranscriber.getIncomingMessageTranscriber(replyType), null, null, null, timeout);
    }

    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            T message,
            Map<String, Object> customHeaders,
            Class<U> replyType) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, customHeaders, messageTranscriber.getIncomingMessageTranscriber(replyType), null, null, null, null);
    }

    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            T message,
            Class<U> replyType,
            Duration timeout) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, null, messageTranscriber.getIncomingMessageTranscriber(replyType), null, null, null, timeout);
    }
    public <T,U> Single<RpcReply<U>> sendRequest(
            Destination destination,
            T message,
            Class<U> replyType) {
        return sendRequest(destination, jmsObjectRepository.getPrivateTempQueue(), message, null, messageTranscriber.getIncomingMessageTranscriber(replyType), null, null, null, null);
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
        java.util.function.Function<Throwable, Boolean> testFunc = ex ->
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
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends CommAdapterBuilder<String, JmsAdapter>{
        private String identifier;
        private Supplier<String> correlationIdGenerator;
        private java.util.function.Function<Destination, String> destinationIdGenerator;
        private ConnectionFactory connectionFactory;
        private boolean consumerSessionTransacted = false;
        private int consumerSessionAckMode = Session.AUTO_ACKNOWLEDGE;
        private boolean producerSessionTransacted = false;
        private int producerSessionAckMode = Session.AUTO_ACKNOWLEDGE;
        private int defaultPriority = Message.DEFAULT_PRIORITY;
        private long defaultTimeToLive = Message.DEFAULT_TIME_TO_LIVE;
        private int defaultDeliveryMode = DeliveryMode.NON_PERSISTENT;
        private JmsObjectRepository jmsObjectRepository;
        private MessageSender messageSender;
        private MessageReceiver messageReceiver;
        private RpcServer rpcServer;
        private RpcClient rpcClient;
        private Duration defaultRpcTimeout;

        private Builder() {
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setDefaultRpcTimeout(Duration defaultRpcTimeout) {
            this.defaultRpcTimeout = defaultRpcTimeout;
            return this;
        }

        public Builder setDefaultMessagePriority(int priority) {
            this.defaultPriority = priority;
            return this;
        }

        public Builder setDefaultMessageTimeToLive(long timeToLive) {
            this.defaultTimeToLive = timeToLive;
            return this;
        }

        public Builder setDefaultMessageDeliveryMode(int deliveryMode) {
            this.defaultDeliveryMode = deliveryMode;
            return this;
        }

        public Builder setConnectionFactory(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        public Builder setProducerSessionTransacted(boolean sessionTransacted) {
            this.producerSessionTransacted = sessionTransacted;
            return this;
        }

        public Builder setProducerSessionAckMode(int sessionAckMode) {
            this.producerSessionAckMode = sessionAckMode;
            return this;
        }

        public Builder setConsumerSessionTransacted(boolean sessionTransacted) {
            this.consumerSessionTransacted = sessionTransacted;
            return this;
        }

        public Builder setConsumerSessionAckMode(int sessionAckMode) {
            this.consumerSessionAckMode = sessionAckMode;
            return this;
        }

        public Builder setCorrelationIdGenerator(Supplier<String> correlationIdGenerator) {
            this.correlationIdGenerator = correlationIdGenerator;
            return this;
        }

        public Builder setDestinationIdGenerator(java.util.function.Function<Destination, String> destinationIdGenerator) {
            this.destinationIdGenerator = destinationIdGenerator;
            return this;
        }

        // for testing
        public Builder setJmsObjectRepository(JmsObjectRepository jmsObjectRepository) {
            this.jmsObjectRepository = jmsObjectRepository;
            return this;
        }

        public Builder setRpcClient(RpcClient rpcClient) {
            this.rpcClient = rpcClient;
            return this;
        }

        public Builder setRpcServer(RpcServer rpcServer) {
            this.rpcServer = rpcServer;
            return this;
        }

        public Builder setMessageReceiver(MessageReceiver messageReceiver) {
            this.messageReceiver = messageReceiver;
            return this;
        }

        public Builder setMessageSender(MessageSender messageSender) {
            this.messageSender = messageSender;
            return this;
        }

        protected void validate() {
            requireNonNull(metrics,"metrics must be provided");

            if (messageTranscriber == null) {
                messageTranscriber = new DefaultMessageTranscriberForStringAsTransportType();
            }

            if (messageSender == null || messageReceiver == null) {
                requireNonNull(connectionFactory, "connectionFactory must be provided");
            }

            if (correlationIdGenerator == null) {
                correlationIdGenerator = new UIDCorrelationIdGenerator();
            }
            if (destinationIdGenerator == null) {
                destinationIdGenerator = new DefaultDestinationIdGenerator();
            }

            if (defaultRpcTimeout == null) {
                defaultRpcTimeout = Duration.ofSeconds(30);
            }
        }

        public JmsAdapter createInstance() {
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
                messageSender = new MessageSender(identifier, jmsObjectRepository, metrics);
            }
            if (messageReceiver == null) {
                messageReceiver = new MessageReceiver(identifier, jmsObjectRepository, metrics);
            }
            if (rpcServer == null) {
                rpcServer = new RpcServer(identifier, messageReceiver, messageSender, metrics);
            }
            if (rpcClient==null) {
                rpcClient = new RpcClient(identifier, messageSender, messageReceiver, metrics);
            }
            return new JmsAdapter(this);
        }
    }
}
