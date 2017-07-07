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

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageDetails;
import ch.squaredesk.nova.comm.retrieving.MessageReceiver;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KafkaMessageReceiver<InternalMessageType>
        extends MessageReceiver<String, InternalMessageType, String, KafkaSpecificInfo> {

    private final Logger logger = LoggerFactory.getLogger(KafkaMessageReceiver.class);

    private final KafkaObjectFactory kafkaObjectFactory;
    private final Scheduler schedulerToSubscribeOn;

    KafkaMessageReceiver(String identifier,
                         KafkaObjectFactory kafkaObjectFactory,
                         Scheduler schedulerToSubscribeOn,
                         MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                         Metrics metrics) {
        super(identifier, messageUnmarshaller, metrics);
        this.kafkaObjectFactory = kafkaObjectFactory;
        this.schedulerToSubscribeOn = schedulerToSubscribeOn;
    }


    private List<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>> convert(ConsumerRecords<String, String> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        List<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>> returnValue = new ArrayList<>();

        Iterator<ConsumerRecord<String, String>> iterator = records.iterator();
        while (iterator.hasNext()) {
            ConsumerRecord<String,String> record = iterator.next();
            try {
                InternalMessageType internalMessage = messageUnmarshaller.unmarshal(record.value());
                // FIXME: which data?
                KafkaSpecificInfo kafkaSpecificInfo = new KafkaSpecificInfo();
                IncomingMessageDetails<String, KafkaSpecificInfo> messageDetails = new IncomingMessageDetails.Builder<String, KafkaSpecificInfo>()
                        .withDestination(record.topic())
                        .withTransportSpecificDetails(kafkaSpecificInfo)
                        .build();

                returnValue.add(new IncomingMessage<>(internalMessage,messageDetails));
            } catch (Throwable t) {
                logger.error("Unable to parse incoming message " + record, t);
            }
        }
        return returnValue;
    }

    @Override
    public Observable<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>>
        doSubscribe(String destination) {

        return Observable.<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>>create(subscription -> {
            long pollTimeout = 1; // FIXME: field
            TimeUnit pollTimeUnit = TimeUnit.SECONDS;  // FIXME: field
            KafkaPoller kafkaPoller = kafkaObjectFactory.pollerForTopic(destination, pollTimeout, pollTimeUnit);
            kafkaPoller.setRecordsConsumer(records -> {
                List<IncomingMessage<InternalMessageType, String, KafkaSpecificInfo>> incomingMessages = convert(records);
                incomingMessages.forEach(message -> subscription.onNext(message));
            });
            kafkaPoller.start();
            logger.info("Subscribed to topic " + destination);
        }).subscribeOn(schedulerToSubscribeOn);
    }

    @Override
    protected void doUnsubscribe(String destination)  {
        kafkaObjectFactory.destroyPollerForTopic(destination);
        logger.info("Unsubscribed from topic " + destination);
    }

}
