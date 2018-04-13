/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.rpc;


import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RpcRequestProcessor<MessageType, RpcInvocationType extends RpcInvocation<? extends MessageType, ?, ? extends MessageType, ?>>
        implements Consumer<RpcInvocationType> {

    private static final Logger logger = LoggerFactory.getLogger(RpcRequestProcessor.class);

    private final Map<Class<?>, BiConsumer<? extends MessageType, RpcInvocationType>> handlerFunctions = new ConcurrentHashMap<>();
    private final RpcServerMetricsCollector metricsCollector;

    private java.util.function.Consumer<RpcInvocationType> unregisteredRequestHandler = invocation -> {
        logger.error("No handler found to process incoming request " + invocation);
        invocation.completeExceptionally(new RuntimeException("Invalid request"));
    };
    private BiConsumer<RpcInvocationType, Throwable> uncaughtExceptionHandler = (invocation, error) -> {
        logger.error("An error occurred, trying to process incoming request " + invocation, error);
        invocation.completeExceptionally(new RuntimeException("Invalid request"));
    };

    public RpcRequestProcessor(Metrics metrics) {
        Objects.requireNonNull(metrics, "Metrics must not be null");
        metricsCollector = new RpcServerMetricsCollector(null, metrics);
    }

    public void register (Class<?> requestClass,
                          BiConsumer<? extends MessageType, RpcInvocationType> handlerFunction) {
        if (handlerFunctions.containsKey(requestClass)) {
            throw new IllegalArgumentException("Handler for request type " + requestClass.getName() + " already registered");
        }
        handlerFunctions.put(requestClass, handlerFunction);
    }

    @Override
    public void accept (RpcInvocationType rpcInvocation) {
        try {
            BiConsumer<MessageType, RpcInvocationType> handlerFunction = null;
            if (rpcInvocation.request != null && rpcInvocation.request.message != null) {
                handlerFunction = (BiConsumer<MessageType, RpcInvocationType>) handlerFunctions.get(rpcInvocation.request.message.getClass());
            }

            if (handlerFunction==null) {
                unregisteredRequestHandler.accept(rpcInvocation);
            } else {
                metricsCollector.requestReceived(rpcInvocation.request);
                MessageType request = rpcInvocation.request.message;
                handlerFunction.accept(request, rpcInvocation);
                metricsCollector.requestCompleted(rpcInvocation.request, null);
            }
        } catch (Throwable t) {
            logger.error("An error occurred, trying to process incoming request " + rpcInvocation, t);
            metricsCollector.requestCompletedExceptionally(rpcInvocation.request, t);
            rpcInvocation.completeExceptionally(new RuntimeException("Invalid request"));
        }
    }

    public void onUnregisteredRequest(java.util.function.Consumer<RpcInvocationType> function) {
        Objects.requireNonNull(function, "handler for unregistered requests must not be null");
        this.unregisteredRequestHandler = function;
    }

    public void onProcessingException(BiConsumer<RpcInvocationType, Throwable> function) {
        Objects.requireNonNull(function, "handler for processing errors must not be null");
        this.uncaughtExceptionHandler = function;
    }

}
