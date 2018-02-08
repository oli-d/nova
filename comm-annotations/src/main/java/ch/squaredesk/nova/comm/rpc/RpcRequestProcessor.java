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


import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;


public class RpcRequestProcessor<RpcInvocationType extends RpcInvocation<IncomingMessageType, ?, ReturnMessageType, ?>, IncomingMessageType, ReturnMessageType>
        implements Function<RpcInvocationType, Pair<RpcInvocationType, ReturnMessageType>> {

    private static final Logger logger = LoggerFactory.getLogger(RpcRequestProcessor.class);

    private final Map<Class<?>, Function<IncomingMessageType, ReturnMessageType>> handlerFunctions = new ConcurrentHashMap<>();

    private java.util.function.Function<IncomingMessageType, ReturnMessageType> onMissingHandler;
    private BiFunction<IncomingMessageType, Throwable, ReturnMessageType> onProcessingException;

    public void register (Class<?> requestClass,
                          Function<IncomingMessageType, ReturnMessageType> handlerFunction) {
        if (handlerFunctions.containsKey(requestClass)) {
            throw new IllegalArgumentException("Handler for request type " + requestClass.getName() + " already registered");
        }
        handlerFunctions.put(requestClass, handlerFunction);
    }

    @Override
    public Pair<RpcInvocationType, ReturnMessageType> apply(RpcInvocationType rpcInvocation) throws Exception {
        Function<IncomingMessageType, ReturnMessageType> handlerFunction =
                handlerFunctions.get(rpcInvocation.request.getClass());

        ReturnMessageType reply = null;

        if (handlerFunction==null) {
            if (onMissingHandler!=null) {
                reply = onMissingHandler.apply(rpcInvocation.request);
            } else {
                if (onProcessingException!=null) {
                    reply = onProcessingException.apply(
                            rpcInvocation.request,
                            new IllegalArgumentException("Requests of type " + rpcInvocation.request.getClass().getSimpleName() + " are not supported"));
                } else {
                    logger.error("No handler found to process incoming request " + rpcInvocation);
                }
            }
        } else {
            try {
                reply = handlerFunction.apply(rpcInvocation.request);
            } catch (Throwable t) {
                if (onProcessingException != null) {
                    reply = onProcessingException.apply(rpcInvocation.request, t);
                } else {
                    logger.error("An error occurred, trying to process incoming request " + rpcInvocation, t);
                }
            }
        }

        return new Pair<>(rpcInvocation, reply);
    }

    public void onMissingRequestProcessor(java.util.function.Function<IncomingMessageType, ReturnMessageType> function) {
        this.onMissingHandler = function;
    }

    public void onProcessingException(BiFunction<IncomingMessageType, Throwable, ReturnMessageType> function) {
        this.onProcessingException = function;
    }
}
