/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.tuples.Pair;
import com.codahale.metrics.Timer;
import io.reactivex.*;
import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.io.NIOReader;
import org.glassfish.grizzly.http.io.NIOWriter;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ch.squaredesk.nova.comm.http.MetricsCollectorInfoCreator.createInfoFor;

public class RpcServer extends ch.squaredesk.nova.comm.rpc.RpcServer<String, String> implements HttpServerInstanceListener {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private final Map<String, Flowable<RpcInvocation>> mapDestinationToIncomingMessages = new ConcurrentHashMap<>();

    private HttpServer httpServer;

    public RpcServer(HttpServer httpServer, MessageTranscriber<String> messageTranscriber, Metrics metrics) {
        this(null, httpServer, messageTranscriber, metrics);
    }

    public RpcServer(HttpServer httpServer, Metrics metrics) {
        this(null, httpServer, new DefaultMessageTranscriberForStringAsTransportType(), metrics);
    }

    public RpcServer(String identifier, HttpServer httpServer, Metrics metrics) {
        this(identifier, httpServer, new DefaultMessageTranscriberForStringAsTransportType(), metrics);
    }
    public RpcServer(String identifier, HttpServer httpServer, MessageTranscriber<String> messageTranscriber, Metrics metrics) {
        super(Metrics.name("http", identifier).toString(), messageTranscriber, metrics);
        this.httpServer = httpServer;
    }

    @Override
    public <T> Flowable<RpcInvocation<T>> requests(String destination, Class<T> targetType) {
        URL destinationAsLocalUrl;
        try {
            destinationAsLocalUrl = new URL("http", "localhost", destination);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Flowable retVal = mapDestinationToIncomingMessages
                .computeIfAbsent(destination, key -> {
                    logger.info("Listening to requests on {}", destination);

                    Flowable<RequestResponseInfoHolder> httpInvocations = Flowable.create(
                        s -> {
                            HttpHandler httpHandler = new HttpHandler() {
                                @Override
                                public void service(Request request, Response response) {
                                    s.onNext(new RequestResponseInfoHolder(destinationAsLocalUrl, request, response));
                                }
                            };
                            httpServer.getServerConfiguration().addHttpHandler(httpHandler, destination);
                        },
                        BackpressureStrategy.BUFFER
                    );

                    return httpInvocations
                            .map(requestResponseInfoHolder -> {
                                Single<T> requestObject =
                                        requestString(requestResponseInfoHolder.request.getNIOReader())
                                        .map(requestAsString -> requestAsString.trim().isEmpty() ? null : messageTranscriber.getIncomingMessageTranscriber(targetType).apply(requestAsString);
);
                                return requestResponseInfoHolder.addRequestObject(requestObject);
                            })
                            .map(requestResponseInfoHolder -> {
                                return new RpcInvocation<>(
                                        new IncomingMessage<>(incomingMessage, metaData),
                                        responseWriter,
                                        error -> {
                                            logger.error("An error occurred trying to process HTTP request {}", incomingMessage, error);
                                            try {
                                                processingErrorConsumer.accept(error);
                                            } catch (Exception any) {
                                                logger.error("Failed to send error 500 back to client", any);
                                            }
                                        },
                                        messageTranscriber
                                );
                            })


                    return Flowable.<RpcInvocation>create(
                        s -> {
                            NonBlockingHttpHandler httpHandler = new NonBlockingHttpHandler(destinationAsLocalUrl, messageTranscriber, targetType, s);
                            httpServer.getServerConfiguration().addHttpHandler(httpHandler, destination);
                        },
                        BackpressureStrategy.BUFFER
                    )
                    .doFinally(() -> {
                        mapDestinationToIncomingMessages.remove(destination);
                        // FIXME: httpServer.getServerConfiguration().removeHttpHandler(httpHandler);
                        logger.info("Stopped listening to requests on {} ", destination);
                    })
                    .share();
                });
        return (Flowable<RpcInvocation<T>>)retVal;
    }

    private static Single<String> requestString(NIOReader inputReader) {
        return Observable.<String>create(
                s -> inputReader.notifyAvailable(createRequestPartEmittingReadHandler(inputReader, s))
        )
        .collect(StringBuffer::new, StringBuffer::append)
        .map(StringBuffer::toString)
        ;
    }

    private static ReadHandler createRequestPartEmittingReadHandler(NIOReader inputReader, ObservableEmitter<String> observableEmitter) {
        return new ReadHandler() {
            @Override
            public void onDataAvailable() throws Exception {
                // we are not synchronizing here, since we assume that onDataAvailable() is called sequentially
                char[] readBuffer = new char[inputReader.readyData()];
                int numRead = inputReader.read(readBuffer);
                if (numRead <= 0) {
                    observableEmitter.onComplete();
                } else {
                    observableEmitter.onNext(new String(readBuffer));  // TODO: we do not supply character encoding anywhere
                }

                inputReader.notifyAvailable(this);
            }

            @Override
            public void onError(Throwable t) {
                observableEmitter.onError(t);
            }

            @Override
            public void onAllDataRead() throws Exception {
                observableEmitter.onComplete();
            }
        };
    }

    private static Consumer<Pair<String, ReplyInfo>> createResponseWriter(Response response, Timer.Context timerContext) {
        return replyInfoPair -> {
            response.setCharacterEncoding("utf-8");
            try (NIOWriter out = response.getNIOWriter()) {
                response.setContentType("application/json");
                response.setContentLength(replyInfoPair._1.length());
                int statusCode;
                if (replyInfoPair._2 == null) {
                    statusCode = 200;
                } else {
                    replyInfoPair._2.headerParams.forEach(response::setHeader);
                    statusCode = replyInfoPair._2.statusCode;
                }
                response.setStatus(statusCode);

                // write response
                try (BufferedWriter writer = new BufferedWriter(out)) {
                    System.out.println("Sending back " + replyInfoPair + " to " + response + " on " + Thread.currentThread().getName());
                    writer.write(replyInfoPair._1);
                    System.out.println("            Write complete " + response);
                }

                metricsCollector.requestCompleted(timerContext, replyInfoPair._1);
            } catch (Exception e) {
                metricsCollector.requestCompletedExceptionally(timerContext, createInfoFor(destination), e);
                logger.error("An error occurred trying to send HTTP response {}", replyInfoPair._2, e);
                try {
                    response.sendError(500, "Internal server error");
                } catch (Exception any) {
                    logger.error("Failed to send error 500 back to client", any);
                }
            } finally {
                // TODO - this is from the example. Do we want to do something here?
                response.resume();
            }
        };
    }


    boolean isStarted() {
        return httpServer !=null && httpServer.isStarted();
    }

    void start() throws IOException {
        httpServer.start();
    }

    void shutdown() {
        if (httpServer != null) {
            try {
                httpServer.shutdown(2, TimeUnit.SECONDS).get();
            } catch (Exception e) {
                logger.info("An error occurred, trying to shutdown REST HTTP server", e);
            }
        }
    }

    @Override
    public void httpServerInstanceCreated(HttpServer httpServer) {
        if (this.httpServer == null) {
            logger.info("httpServer available, RpcServer functional");
        }
        this.httpServer = httpServer;
    }

    private class NonBlockingHttpHandler<T> extends HttpHandler {
        private final URL destination;
        private final MessageTranscriber<String> messageTranscriber;
        private final Class<T> targetType;
        private final FlowableEmitter<Pair<Request, Response>> flowableEmitter;

        private NonBlockingHttpHandler(
                URL destination,
                MessageTranscriber<String> messageTranscriber,
                Class<T> targetType,
                FlowableEmitter<RpcInvocation<T>> flowableEmitter) {
            this.destination = destination;
            this.messageTranscriber = messageTranscriber;
            this.targetType = targetType;
            this.flowableEmitter = flowableEmitter;
        }

        @Override
        public void service(Request request, Response response) {
            flowableEmitter.onNext(Pair.create(request, response));
        }

        public void serviceDeleteMe(Request request, Response response) {
            response.suspend();
            NIOReader requestReader = request.getNIOReader();
            requestString(requestReader)
                .map(s -> {
                    try {
                        requestReader.close();
                    } catch (Exception ignored) {
                        // TODO - this is from the example. Do we want to do something here?
                    }
                    return s.trim();
                })
                .map(requestAsString -> {
                    T requestObject = requestAsString.trim().isEmpty() ? null : messageTranscriber.getIncomingMessageTranscriber(targetType).apply(requestAsString);
                    RequestInfo requestInfo = httpSpecificInfoFrom(request);
                    RequestMessageMetaData metaData = new RequestMessageMetaData(destination, requestInfo);
                    Timer.Context timerContext = metricsCollector.requestReceived(createInfoFor(destination));
                    return createRpcInvocationObjectForReply(requestObject, metaData,
                            createResponseWriter(response, timerContext),
                            error -> response.sendError(500, "Internal server error")
                    );
                })
                .subscribe(
                        invocation -> {
                            // System.out.println("On next()ing");
                            flowableEmitter.onNext(invocation);
                            System.out.println("            On next()ing done " + invocation + " on " + Thread.currentThread().getName());
                        },
                        error -> flowableEmitter.onError(error)
                )
            ;
        }

        private Consumer<Pair<String, ReplyInfo>> createResponseWriter(Response response, Timer.Context timerContext) {
            return replyInfoPair -> {
                response.setCharacterEncoding("utf-8");
                try (NIOWriter out = response.getNIOWriter()) {
                    response.setContentType("application/json");
                    response.setContentLength(replyInfoPair._1.length());
                    int statusCode;
                    if (replyInfoPair._2 == null) {
                        statusCode = 200;
                    } else {
                        replyInfoPair._2.headerParams.forEach(response::setHeader);
                        statusCode = replyInfoPair._2.statusCode;
                    }
                    response.setStatus(statusCode);

                    // write response
                    try (BufferedWriter writer = new BufferedWriter(out)) {
                        System.out.println("Sending back " + replyInfoPair + " to " + response + " on " + Thread.currentThread().getName());
                        writer.write(replyInfoPair._1);
                        System.out.println("            Write complete " + response);
                    }

                    metricsCollector.requestCompleted(timerContext, replyInfoPair._1);
                } catch (Exception e) {
                    metricsCollector.requestCompletedExceptionally(timerContext, createInfoFor(destination), e);
                    logger.error("An error occurred trying to send HTTP response {}", replyInfoPair._2, e);
                    try {
                        response.sendError(500, "Internal server error");
                    } catch (Exception any) {
                        logger.error("Failed to send error 500 back to client", any);
                    }
                } finally {
                    // TODO - this is from the example. Do we want to do something here?
                    response.resume();
                }
            };
        }

        private Single<String> requestString(NIOReader inputReader) {
            return Observable.<char[]>create(
                        s -> inputReader.notifyAvailable(createReadHandlerEmittingResponsePart(inputReader, s))
                    )
                    .map(String::new) // we do not supply character encoding anywhere
                    .collect(StringBuffer::new, StringBuffer::append)
                    .map(StringBuffer::toString)
                    ;
        }

        private ReadHandler createReadHandlerEmittingResponsePart(NIOReader inputReader, ObservableEmitter<char[]> observableEmitter) {
            return new ReadHandler() {
                @Override
                public void onDataAvailable() throws Exception {
                    // we are not synchronizing here, since we assume that onDataAvailable() is called sequentially
                    char[] readBuffer = new char[inputReader.readyData()];
                    int numRead = inputReader.read(readBuffer);
                    if (numRead <= 0) {
                        observableEmitter.onComplete();
                    } else {
                        observableEmitter.onNext(readBuffer);
                    }

                    inputReader.notifyAvailable(this);
                }

                @Override
                public void onError(Throwable t) {
                    observableEmitter.onError(t);
                }

                @Override
                public void onAllDataRead() throws Exception {
                    observableEmitter.onComplete();
                }
            };
        }

        private RequestInfo httpSpecificInfoFrom(Request request) {
            Map<String, String> parameters = new HashMap<>();
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                String[] valueList = entry.getValue();
                String valueToPass = null;
                if (valueList != null && valueList.length > 0) {
                    valueToPass = valueList[0];
                }
                parameters.put(entry.getKey(), valueToPass);
            }

            return new RequestInfo(
                    convert(request.getMethod()), parameters);
        }

        private RpcInvocation<T> createRpcInvocationObjectForReply(T incomingMessage,
                                                                   RequestMessageMetaData metaData,
                                                                   Consumer<Pair<String, ReplyInfo>> responseWriter,
                                                                   io.reactivex.functions.Consumer<Throwable> processingErrorConsumer) {
            return new RpcInvocation<>(
                    new IncomingMessage<>(incomingMessage, metaData),
                    responseWriter,
                    error -> {
                        logger.error("An error occurred trying to process HTTP request {}", incomingMessage, error);
                        try {
                            processingErrorConsumer.accept(error);
                        } catch (Exception any) {
                            logger.error("Failed to send error 500 back to client", any);
                        }
                    },
                    messageTranscriber
            );
        }
    }
}
