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

import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

class JmsObjectRepository {
    private final Logger logger = LoggerFactory.getLogger(JmsObjectRepository.class);

    private final Map<String, MessageProducer> mapDestinationIdToMessageProducer = new ConcurrentHashMap<>();

    private Session producerSession;
    private Session consumerSession;
    private Destination tempQueue;

    private final Connection connection;
    private final JmsSessionDescriptor producerSessionDescriptor;
    private final JmsSessionDescriptor consumerSessionDescriptor;
    private final Function<Destination, String> destinationIdGenerator;

    JmsObjectRepository(Connection connection,
                        JmsSessionDescriptor producerSessionDescriptor,
                        JmsSessionDescriptor consumerSessionDescriptor,
                        Function<Destination, String> destinationIdGenerator) {
        this.connection = connection;
        this.producerSessionDescriptor = producerSessionDescriptor;
        this.consumerSessionDescriptor = consumerSessionDescriptor;
        this.destinationIdGenerator = destinationIdGenerator;
    }

    Destination getPrivateTempQueue() {
        if (tempQueue == null) {
            try {
                tempQueue = consumerSession.createTemporaryQueue();
            } catch (JMSException e) {
                throw new RuntimeException("Unable to create temp queue", e);
            }
        }
        return tempQueue;
    }


    MessageConsumer createMessageConsumer(Destination destination) throws JMSException {
        return consumerSession.createConsumer(destination);
    }

    String idFor (Destination destination) {
        return destinationIdGenerator.apply(destination);
    }

    void destroyConsumer(MessageConsumer consumer) {
        // we defer the closing of the MessageProducer (for the magical amount of 1 second"
        // because we ran into issues (which we do not understand at all) when we close a consumer
        // and very quickly afterwards create a new consumer for the same destination. This is a common
        // scenario during testing. In such a case it happened, that the new consumer never returned any
        // messages. Our theory is that for some reason the ActiveMQ broker runs into timing issues. We
        // do not have an explanation for what happens, but we saw, that if we delay the closing of the
        // no longer needed consumer, we did not experience any problems.
        Observable.timer(1, TimeUnit.SECONDS)
                .subscribe(x -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        consumer.close();
                    } catch (Exception e) {
                        // noop, we did our best
                    }
                });
    }


    TextMessage createTextMessage() throws JMSException {
        return producerSession.createTextMessage();
    }

    MessageProducer createMessageProducer(Destination destination) {
        String destinationId = destinationIdGenerator.apply(destination);
        return mapDestinationIdToMessageProducer.computeIfAbsent(destinationId,
                key -> {
                    try {
                        return producerSession.createProducer(destination);
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    void start() throws JMSException {
        connection.start();
        logger.debug("Creating producer session with the following settings: {}", producerSessionDescriptor);
        this.producerSession = connection.createSession(producerSessionDescriptor.transacted, producerSessionDescriptor.acknowledgeMode);
        logger.debug("Creating consumer session with the following settings: {}", consumerSessionDescriptor);
        this.consumerSession = connection.createSession(consumerSessionDescriptor.transacted, consumerSessionDescriptor.acknowledgeMode);
    }

    void shutdown() {
        mapDestinationIdToMessageProducer.forEach((key, value) -> {
            try {
                value.close();
            } catch (Exception e) {
                logger.warn("Unable to close producer for destination " + key, e);
            }
        });
        mapDestinationIdToMessageProducer.clear();
        try {
            consumerSession.close();
        } catch (Exception e) {
            logger.warn("Unable to close producer session", e);
        }
        try {
            producerSession.close();
        } catch (Exception e) {
            logger.warn("Unable to close producer session", e);
        }
        try {
            connection.close();
        } catch (Exception e) {
            logger.warn("Unable to close connection", e);
        }

    }

}
