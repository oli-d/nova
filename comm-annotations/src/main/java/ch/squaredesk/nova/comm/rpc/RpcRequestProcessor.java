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


import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class RpcRequestProcessor<RpcInvocationType extends RpcInvocation<IncomingMessageType, ?, ReturnMessageType, ?>,
        IncomingMessageType, ReturnMessageType> implements Consumer<RpcInvocationType> {

    private static final Logger logger = LoggerFactory.getLogger(RpcRequestProcessor.class);

    private final Map<Class<?>, Consumer<RpcInvocationType>> handlerFunctions = new ConcurrentHashMap<>();

    private java.util.function.Consumer<RpcInvocationType> unregisteredRequestHandler = invocation -> {
        logger.error("No handler found to process incoming request " + invocation);
    };

    public void register (Class<?> requestClass,
                          Consumer<RpcInvocationType> handlerFunction) {
        if (handlerFunctions.containsKey(requestClass)) {
            throw new IllegalArgumentException("Handler for request type " + requestClass.getName() + " already registered");
        }
        handlerFunctions.put(requestClass, handlerFunction);
    }

    @Override
    public void accept (RpcInvocationType rpcInvocation) throws Exception {
        Consumer<RpcInvocationType> handlerFunction = handlerFunctions.get(rpcInvocation.request.getClass());

        if (handlerFunction==null) {
            unregisteredRequestHandler.accept(rpcInvocation);
        } else {
            try {
                handlerFunction.accept(rpcInvocation);
            } catch (Throwable t) {
                logger.error("An error occurred, trying to process incoming request " + rpcInvocation, t);
            }
        }
    }

    public void onUnregisteredRequest(java.util.function.Consumer<RpcInvocationType> function) {
        Objects.requireNonNull(function, "handler for unregistered requests must not be null");
        this.unregisteredRequestHandler = function;
    }

}
