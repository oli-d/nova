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

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageDetails;
import ch.squaredesk.nova.comm.retrieving.MessageReceiver;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

public class JmsMessageReceiver<InternalMessageType>
        extends MessageReceiver<Destination, InternalMessageType, String, JmsSpecificInfo> {

    private final Logger logger = LoggerFactory.getLogger(JmsMessageReceiver.class);

    private final JmsObjectRepository jmsObjectRepository;

    JmsMessageReceiver(String identifier,
                       JmsObjectRepository<InternalMessageType> jmsObjectRepository,
                       MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                       Metrics metrics) {
        super(identifier, messageUnmarshaller, metrics);
        this.jmsObjectRepository = jmsObjectRepository;
    }

    @Override
    public Flowable<IncomingMessage<InternalMessageType, Destination, JmsSpecificInfo>> messages(Destination destination) {
        try {
            return jmsObjectRepository.messages(destination, messageUnmarshaller);
        } catch (JMSException e) {
            return Flowable.error(e);
        }
    }
}
