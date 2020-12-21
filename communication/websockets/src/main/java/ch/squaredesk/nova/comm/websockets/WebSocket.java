/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.reactivex.rxjava3.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class WebSocket {
    private static final Logger logger = LoggerFactory.getLogger(WebSocket.class);

    private final Subject<String> messages;
    private final Observable<String> messagesToHandOut;

    private final MetricsCollector metricsCollector;
    private final MessageTranscriber<String> messageTranscriber;
    protected final String destination;
    private final String destinationForMetrics;

    private final ConcurrentHashMap<String, Object> userProperties = new ConcurrentHashMap<>(1);

    private final CopyOnWriteArrayList<Consumer<Pair<WebSocket, CloseReason>>> closeHandlers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Pair<WebSocket, Throwable>>> errorHandlers = new CopyOnWriteArrayList<>();

    public WebSocket(String destination,
                     MessageTranscriber<String> messageTranscriber,
                     MetricsCollector metricsCollector) {
        this.destination = destination;
        this.destinationForMetrics = destination.startsWith("/") ? destination.substring(1) : destination;
        this.messageTranscriber = messageTranscriber;
        this.metricsCollector = metricsCollector;

        this.messages = PublishSubject.create();
        this.messagesToHandOut = messages
                .doOnEach(message -> metricsCollector.messageReceived(destinationForMetrics))
                .retry()
                .share();

        metricsCollector.subscriptionCreated(destinationForMetrics);
    }


    public Completable close() {
        return close(CloseReason.NORMAL_CLOSURE);
    }

    public Completable close(CloseReason closeReason) {
        if (!closeReason.mightBeUsedByEndpoint) {
            return Completable.error(new IllegalArgumentException("CloseReason " + closeReason + " cannot be used by endpoints"));
        }
        try {
            doClose(closeReason);
            return Completable.complete();
        } catch (Exception e) {
            return Completable.error(e);
        } finally {
            messages.onComplete();
            propagateCloseEvent(closeReason);
        }
    }

    public void clearUserProperties() {
        userProperties.clear();
    }

    public void setUserProperty(String propertyId, Object value) {
        if (value == null) {
            userProperties.remove(propertyId);
        } else {
            userProperties.put(propertyId, value);
        }
    }

    public String getUserProperty(String propertyId) {
        return getUserProperty(propertyId, String.class);
    }

    public <PropertyType> PropertyType getUserProperty(String propertyId, Class<PropertyType> propertyType) {
        return (PropertyType)userProperties.get(propertyId);
    }

    public <T> Observable<IncomingMessage<T, IncomingMessageMetaData>> messages (Function<String, T> messageTranscriber) {
        return messagesToHandOut
                .map(messageTranscriber)
                .doOnError(error -> metricsCollector.unparsableMessageReceived(destinationForMetrics))
                .retry()
                .map(msg -> new IncomingMessage<>(msg, new IncomingMessageMetaData(destination, new RetrieveInfo(this))));
    }

     public <T> Observable<IncomingMessage<T, IncomingMessageMetaData>> messages (Class<T> messageType) {
        return messages(messageTranscriber.getIncomingMessageTranscriber(messageType));
    }

    public Observable<IncomingMessage<String, IncomingMessageMetaData>> messages () {
        return messages(s -> s);
    }

    public <T> Completable send(T message) {
        try {
            String messageAsString = messageTranscriber.getOutgoingMessageTranscriber(message).apply(message);
            doSend(messageAsString);
            metricsCollector.messageSent(destinationForMetrics);
            return Completable.complete();
        } catch (Throwable e) {
            return Completable.error(e);
        }
    }

    public void onClose(Consumer<Pair<WebSocket, CloseReason>> handler) {
        if (handler != null) {
            closeHandlers.add(handler);
        }
    }

    public void onError(Consumer<Pair<WebSocket, Throwable>> handler) {
        if (handler != null) {
            errorHandlers.add(handler);
        }
    }

    protected void propagateCloseEvent(CloseReason closeReason) {
        metricsCollector.subscriptionDestroyed(destinationForMetrics);
        closeHandlers.forEach(handler -> {
            try {
                handler.accept(new Pair<>(this, closeReason));
            } catch (Throwable e) {
                logger.error("An error occurred, trying to inform handler about close event", e);
            }
        });
    }

    protected void propagateError(Throwable error) {
        errorHandlers.forEach(handler -> {
            try {
                handler.accept(new Pair<>(this, error));
            } catch (Throwable e) {
                logger.error("An error occurred, trying to inform handler about error event", e);
            }
        });
    }

    protected void propagateNewMessage(String message) {
        messages.onNext(message);
    }

    public abstract void doSend(String message);

    protected abstract void doClose(CloseReason closeReason);

    protected abstract boolean isOpen();

}
