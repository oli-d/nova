/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class Endpoint  {
    private final static Logger logger = LoggerFactory.getLogger(Endpoint.class);

    private final String destination;
    private final EndpointStreamSource streamSource;
    private final Consumer<CloseReason> closeAction;
    private final MessageTranscriber<String> messageTranscriber;
    private final MetricsCollector metricsCollector;

    protected Endpoint(String destination, EndpointStreamSource streamSource, MessageTranscriber<String> messageTranscriber, MetricsCollector metricsCollector) {
        this(destination, streamSource, null, messageTranscriber, metricsCollector);
    }

    protected Endpoint(String destination, EndpointStreamSource streamSource, Consumer<CloseReason> closeAction, MessageTranscriber<String> messageTranscriber, MetricsCollector metricsCollector) {
        Objects.requireNonNull(streamSource, "streamSource must not be null");
        this.destination = destination;
        this.streamSource = streamSource;
        this.closeAction = closeAction;
        this.messageTranscriber = messageTranscriber;
        this.metricsCollector = metricsCollector;
    }

    public Flowable<WebSocket> connectedWebSockets() {
        return streamSource.connectingSockets;
    }

    public <T> Flowable<IncomingMessage<T, IncomingMessageMetaData>> messages (Class<T> messageType) {
        if (messageTranscriber == null) {
            throw new IllegalArgumentException("Unable to get message transcriber for class " + messageType);
        }
        return messages(messageTranscriber.getIncomingMessageTranscriber(messageType));
    }

    public <T> Flowable<IncomingMessage<T, IncomingMessageMetaData>> messages (Function<String, T> messageTranscriber) {
        return streamSource
                .messages
                .map(tuple -> {
                    T message = null;
                    IncomingMessageMetaData meta = null;
                    try {
                        String messageAsString = tuple._1;
                        message = messageTranscriber.apply(messageAsString);
                        RetrieveInfo retrieveInfo = new RetrieveInfo(tuple._3);
                        meta = new IncomingMessageMetaData(tuple._2, retrieveInfo);
                        metricsCollector.messageReceived(destination);
                    } catch (Exception e) {
                        logger.error("Unable to transcribe incoming message {} ", tuple._1, e);
                        metricsCollector.unparsableMessageReceived(destination);
                    }
                    return new IncomingMessage<>(message, meta);
                })
                .filter(x -> x.message != null)
                ;
    }

    public Flowable<Pair<WebSocket, Throwable>> errors () {
        return streamSource.errors;
    }

    public Flowable<Pair<WebSocket, CloseReason>> closedWebSockets() {
        return streamSource.closingSockets;
    }

    public void close () {
        close(CloseReason.NORMAL_CLOSURE);
    }

    public void close (CloseReason closeReason) {
        if (!closeReason.mightBeUsedByEndpoint) {
            throw new IllegalArgumentException("CloseReason " + closeReason + " cannot be used by endpoints");
        }
        if (closeAction != null) {
            closeAction.accept(closeReason);
        }
    }
}
