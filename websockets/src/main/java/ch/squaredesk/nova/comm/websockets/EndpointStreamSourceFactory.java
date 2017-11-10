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

import ch.squaredesk.nova.tuples.Pair;
import ch.squaredesk.nova.tuples.Tuple3;
import io.reactivex.Observable;

import java.util.function.Function;

public class EndpointStreamSourceFactory {
    public static <SomeMessageType, SomeWebSocketType> EndpointStreamSource<SomeMessageType> createStreamSourceFor(
            String destination,
            Function<SomeWebSocketType, WebSocket<SomeMessageType>> webSocketFactory,
            StreamCreatingEndpointWrapper<SomeWebSocketType, SomeMessageType> streamCreatingEndpointWrapper,
            MetricsCollector metricsCollector) {

        Observable<Tuple3<SomeMessageType, String, WebSocket<SomeMessageType>>> messages = streamCreatingEndpointWrapper.messages()
                .map(pair -> new Tuple3<>(pair._2, destination, webSocketFactory.apply(pair._1)))
                .doOnNext(tuple -> metricsCollector.messageReceived(destination));
        Observable<WebSocket<SomeMessageType>> connectingSockets = streamCreatingEndpointWrapper.connectingSockets()
                .map(webSocketFactory::apply)
                .doOnNext(socket -> metricsCollector.subscriptionCreated(destination));
        Observable<Pair<WebSocket<SomeMessageType>, CloseReason>> closingSockets = streamCreatingEndpointWrapper.closingSockets()
                .map(pair -> new Pair<>(webSocketFactory.apply(pair._1), pair._2))
                .doOnNext(socket -> metricsCollector.subscriptionDestroyed(destination));
        Observable<Pair<WebSocket<SomeMessageType>, Throwable>> errors = streamCreatingEndpointWrapper.errors()
                .map(pair -> new Pair<>(webSocketFactory.apply(pair._1), pair._2)); // TODO metric?
        return new EndpointStreamSource<>(
                messages,
                connectingSockets,
                closingSockets,
                errors);
    }
}
