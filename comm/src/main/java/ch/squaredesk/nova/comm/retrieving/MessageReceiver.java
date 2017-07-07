/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.retrieving;

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public abstract class MessageReceiver<DestinationType, InternalMessageType, TransportMessageType, TransportSpecificInfoType> {
    private static final Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

    private final ConcurrentHashMap<DestinationType, Observable<IncomingMessage<InternalMessageType, DestinationType, TransportSpecificInfoType>>> mapDestinationToListeners = new ConcurrentHashMap<>();
    protected final MessageUnmarshaller<TransportMessageType, InternalMessageType> messageUnmarshaller;
    private final MetricsCollector metricsCollector;

    protected MessageReceiver(MessageUnmarshaller<TransportMessageType, InternalMessageType> messageUnmarshaller, Metrics metrics) {
        this(null, messageUnmarshaller, metrics);
    }

    protected MessageReceiver(String identifier, MessageUnmarshaller<TransportMessageType, InternalMessageType> messageUnmarshaller, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        requireNonNull(messageUnmarshaller, "messageUnmarshaller must not be null");
        this.messageUnmarshaller = messageUnmarshaller;
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

    /**
     * @param destination
     * @return an observable that constantly onNext()s new messages whenever they arrive on the passed destination. Note
     * that the returned Observable does not have to make sure that unsubscription is triggered when the last observer
     * goes away. This is automatically handled
     */
    protected abstract Observable<IncomingMessage<InternalMessageType, DestinationType, TransportSpecificInfoType>>
        doSubscribe(DestinationType destination);

    /**
     * @param destination
     */
    protected abstract void doUnsubscribe(DestinationType destination);

    public Flowable<IncomingMessage<InternalMessageType, DestinationType, TransportSpecificInfoType>>
        messages(DestinationType destination, BackpressureStrategy backpressureStrategy) {
        requireNonNull(destination, "Destination must not be null");
        requireNonNull(backpressureStrategy, "backpressureStrategy must not be null");

        Observable<IncomingMessage<InternalMessageType, DestinationType, TransportSpecificInfoType>> source =
                mapDestinationToListeners.computeIfAbsent(destination, key -> {
                    Observable<IncomingMessage<InternalMessageType, DestinationType, TransportSpecificInfoType>> observable =
                            doSubscribe(destination);
                    metricsCollector.subscriptionCreated(destination);

                    return observable
                            .doOnNext(x -> metricsCollector.messageReceived(destination))
                            .doFinally(() -> {
                                logger.info("Closing connection to " + destination);
                                try {
                                    mapDestinationToListeners.remove(destination);
                                    doUnsubscribe(destination);
                                    metricsCollector.subscriptionDestroyed(destination);
                                } catch (Throwable t) {
                                    logger.error("An error occurred, trying to close the connection to " + destination,t);
                                }
                            })
                            .share();
                });

        return source.toFlowable(backpressureStrategy);
    }

}
