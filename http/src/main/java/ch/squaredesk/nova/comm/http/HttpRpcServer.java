/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageDetails;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.comm.rpc.RpcServer;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.apache.http.*;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

class HttpRpcServer<InternalMessageType> extends RpcServer<String, InternalMessageType> {
    private final Logger logger = LoggerFactory.getLogger(HttpRpcServer.class);
    private final HttpServer httpServer;
    private final IncomingRequestHandler incomingRequestandler;
    private final MessageMarshaller<InternalMessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller;
    private final Function<Throwable, InternalMessageType> errorReplyFactory;

    HttpRpcServer(String identifier,
                  int port,
                  MessageMarshaller<InternalMessageType, String> messageMarshaller,
                  MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                  Function<Throwable, InternalMessageType> errorReplyFactory,
                  Metrics metrics) {
        super(identifier, metrics);

        requireNonNull(messageMarshaller, "messageMarshaller must not be null");
        requireNonNull(messageUnmarshaller, "messageUnmarshaller must not be null");
        requireNonNull(errorReplyFactory, "errorReplyFactory must not be null");

        this.messageMarshaller = messageMarshaller;
        this.messageUnmarshaller = messageUnmarshaller;
        this.errorReplyFactory = errorReplyFactory;

        // FIXME: SSL support
        /*
        SSLContext sslcontext = null;
        if (port == 8443) {
            // Initialize SSL context
            URL url = getClass().getResource("/my.keystore");
            if (url == null) {
                System.out.println("Keystore not found");
                System.exit(1);
            }
            sslcontext = SSLContexts.custom()
                    .loadKeyMaterial(url, "secret".toCharArray(), "secret".toCharArray())
                    .build();
        }
        */

        this.incomingRequestandler = new IncomingRequestHandler();
        // FIXME: what are we configuring here?
        IOReactorConfig config = IOReactorConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        httpServer = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                // FIXME .setServerInfo("Test/1.1")
                .setIOReactorConfig(config)
                // FIXME .setSslContext(sslcontext)
                .setExceptionLogger(ex -> logger.error("HttpServer caught exception", ex))
                // FIXME: interface name
                // FIXME: baseUrl
                .registerHandler("*", incomingRequestandler)
                .create();
    }

    void start() throws Exception {
        httpServer.start();
    }

    void shutdown() {
        httpServer.shutdown(2, TimeUnit.SECONDS);
    }

    @Override
    public <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
        Flowable<RpcInvocation<RequestType, ReplyType>> requests(String destination, BackpressureStrategy backpressureStrategy) {
        Flowable<RpcInvocation<RequestType, ReplyType>> requests = Flowable.create(
                s -> incomingRequestandler.registerIncomingRequestHandler(
                        destination,
                        request -> s.onNext((RpcInvocation<RequestType, ReplyType>) request)),
                backpressureStrategy);
        return requests.doFinally(() -> incomingRequestandler.deregisterIncomingRequestHandler(destination));
    }

    private class IncomingRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {
        private final ConcurrentHashMap<String, Consumer<RpcInvocation<InternalMessageType, InternalMessageType>>>
                destinationToHandler = new ConcurrentHashMap<>();

        @Override
        public HttpAsyncRequestConsumer<HttpRequest> processRequest(
                final HttpRequest request,
                final HttpContext context) {
            // Buffer request content in memory for simplicity
            // FIXME: something better?
            return new BasicAsyncRequestConsumer();
        }

        @Override
        public void handle(
                final HttpRequest httpRequest,
                final HttpAsyncExchange httpExchange,
                final HttpContext context) throws HttpException, IOException {

            String method = httpRequest.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }

            HttpSpecificInfo httpSpecificInfo = new HttpSpecificInfo(
                    HttpRequestMethod.valueOf(httpRequest.getRequestLine().getMethod().toUpperCase()));
            IncomingMessageDetails<String, HttpSpecificInfo> details = new IncomingMessageDetails.Builder<String, HttpSpecificInfo>()
                    .withDestination(httpRequest.getRequestLine().getUri())
                    .withTransportSpecificDetails(httpSpecificInfo)
                    .build();

            InternalMessageType request = requestFrom(httpRequest);
            IncomingMessage<InternalMessageType, String, HttpSpecificInfo> incomingRequest =
                    new IncomingMessage<>(request, details);

            RpcInvocation<InternalMessageType, InternalMessageType> invocation = new RpcInvocation<>(
                    incomingRequest.message,
                    reply -> {
                        sendResponse(request, reply, 200, httpExchange);
                        metricsCollector.requestCompleted(details.destination, reply);
                    },
                    error -> {
                        metricsCollector.requestCompletedExceptionally(details.destination, error);
                        sendResponse(request, errorReplyFactory.apply(error), 400, httpExchange);
                    });
            informHandler(details.destination, invocation);
        }

        private InternalMessageType requestFrom(HttpRequest request) throws IOException {
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                String payload = EntityUtils.toString(entity);
                try {
                    return messageUnmarshaller.unmarshal(payload);
                } catch (Exception e) {
                    throw new IOException("Unable to unmarshal incoming message " + payload, e);
                }
            }
            return null;
        }


        private void sendResponse (InternalMessageType request, InternalMessageType response, int statusCode, HttpAsyncExchange httpExchange) {
            try {
                HttpResponse httpResponse = httpExchange.getResponse();
                String replyAsString = messageMarshaller.marshal(response);
                NStringEntity entity = new NStringEntity(replyAsString, "UTF-8");
                httpResponse.setEntity(entity);
                httpResponse.setStatusCode(statusCode);
                httpExchange.submitResponse(new BasicAsyncResponseProducer(httpResponse));
            } catch (Exception e) {
                logger.error("Unable to send response " + response + " with status code " + statusCode +
                        " for request " + request, e);
            }
        }

        void registerIncomingRequestHandler(
                String destination,
                Consumer<RpcInvocation<InternalMessageType, InternalMessageType>> requestHandler) {
            // FIXME: check for presence!
            destinationToHandler.put(destination, requestHandler);
        }

        private void informHandler(String destination,
                                   RpcInvocation<InternalMessageType, InternalMessageType> incomingRequest) {

            Consumer<RpcInvocation<InternalMessageType, InternalMessageType>> handler = destinationToHandler.get(destination);
            if (handler==null) {
                logger.info("Received request on unsupported destination " + destination + ". Returning error");
                incomingRequest.completeExceptionally(new RuntimeException("Unsupported destination"));
                return;
            }

            try {
                handler.accept(incomingRequest);
            } catch (Throwable t) {
                logger.error("Error, trying to notify handler about incoming request " + incomingRequest, t);
            }
        }

        public void deregisterIncomingRequestHandler(String destination) {
            destinationToHandler.remove(destination);
        }
    }
}
