package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.spring.HttpServerBeanListener;
import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.Method;
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

public class RpcServer extends ch.squaredesk.nova.comm.rpc.RpcServer<String, String> implements HttpServerBeanListener {
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
        super(identifier, messageTranscriber, metrics);
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
                    logger.info("Listening to requests on " + destination);

                    Subject<RpcInvocation> stream = PublishSubject.create();
                    stream = stream.toSerialized();
                    NonBlockingHttpHandler httpHandler = new NonBlockingHttpHandler(destinationAsLocalUrl, messageTranscriber, targetType, stream);

                    httpServer.getServerConfiguration().addHttpHandler(httpHandler, destination);

                    return stream.toFlowable(BackpressureStrategy.BUFFER)
                            .doFinally(() -> {
                                mapDestinationToIncomingMessages.remove(destination);
                                httpServer.getServerConfiguration().removeHttpHandler(httpHandler);
                                logger.info("Stopped listening to requests on " + destination);
                            })
                            .share();
                });
        return retVal;
    }

    private static RequestInfo httpSpecificInfoFrom(Request request) throws Exception {
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

    private static HttpRequestMethod convert(Method method) {
        if (method == Method.CONNECT) {
            return HttpRequestMethod.CONNECT;
        } else if (method == Method.DELETE) {
            return HttpRequestMethod.DELETE;
        } else if (method == Method.GET) {
            return HttpRequestMethod.GET;
        } else if (method == Method.HEAD) {
            return HttpRequestMethod.HEAD;
        } else if (method == Method.OPTIONS) {
            return HttpRequestMethod.OPTIONS;
        } else if (method == Method.PATCH) {
            return HttpRequestMethod.PATCH;
        } else if (method == Method.PRI) {
            return HttpRequestMethod.PRI;
        } else if (method == Method.POST) {
            return HttpRequestMethod.POST;
        } else if (method == Method.PUT) {
            return HttpRequestMethod.PUT;
        } else if (method == Method.TRACE) {
            return HttpRequestMethod.TRACE;
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method " + method);
        }
    }

    /**
     * writes reply Object to response body. Assumes that the marshaller creates a String that is a JSON
     * representation of the reply object
     */
    private static void writeResponse(String reply, NIOWriter out) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(out)) {
            writer.write(reply);
        }
    }

    /**
     * Non-blockingly reads all (non blockingly) available characters of the passed InputReader and
     * concats those characters to the passed <currentBuffer>, returning a new character array
     */
    private static char[] appendAvailableDataToBuffer(NIOReader in, char currentBuffer[]) throws IOException {
        // we are not synchronizing here, since we assume that onDataAvailable() is called sequentially
        char[] readBuffer = new char[in.readyData()];
        int numRead = in.read(readBuffer);
        if (numRead <= 0) {
            return currentBuffer;
        } else {
            char[] retVal = new char[currentBuffer.length + numRead];
            System.arraycopy(currentBuffer, 0, retVal, 0, currentBuffer.length);
            System.arraycopy(readBuffer, 0, retVal, currentBuffer.length, numRead);
            return retVal;
        }
    }

    void start() throws IOException {
        httpServer.start();
    }

    void shutdown() {
        try {
            httpServer.shutdown(2, TimeUnit.SECONDS).get();
        } catch (Exception e) {
            logger.info("An error occurred, trying to shutdown REST HTTP server", e);
        }
    }

    @Override
    public void httpServerAvailableInContext(HttpServer httpServer) {
        if (this.httpServer == null) {
            logger.info("httpServer available, RpcServer functional");
        }
        this.httpServer = httpServer;
    }

    private class NonBlockingHttpHandler<IncomingMessageType> extends HttpHandler {
        private final URL destination;
        private final MessageTranscriber<String> messageTranscriber;
        private final Class<IncomingMessageType> targetType;
        private final Subject<RpcInvocation> stream;

        private NonBlockingHttpHandler(
                URL destination,
                MessageTranscriber<String> messageTranscriber,
                Class<IncomingMessageType> targetType,
                Subject<RpcInvocation> stream) {
            this.destination = destination;
            this.messageTranscriber = messageTranscriber;
            this.targetType = targetType;
            this.stream = stream;
        }

        public void service(Request request, Response response) throws Exception {
            response.suspend();

            NIOReader in = request.getNIOReader();
            in.notifyAvailable(new ReadHandler() {
                private char[] inputBuffer = new char[0];

                @Override
                public void onDataAvailable() throws Exception {
                    inputBuffer = appendAvailableDataToBuffer(in, inputBuffer);
                    in.notifyAvailable(this);
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("Error parsing request data", t);
                    response.setStatus(400, "Bad request");
                    response.resume();
                }

                @Override
                public void onAllDataRead() throws Exception {
                    inputBuffer = appendAvailableDataToBuffer(in, inputBuffer);
                    String incomingMessageAsString = new String(inputBuffer);
                    try {
                        in.close();
                    } catch (Exception ignored) {
                        // TODO - this is from the example. Do we want to do something here?
                    }

                    IncomingMessageType incomingMessage = incomingMessageAsString.trim().isEmpty() ?
                            null :
                            messageTranscriber.getIncomingMessageTranscriber(targetType).apply(incomingMessageAsString);
                    RequestInfo requestInfo = httpSpecificInfoFrom(request);
                    RequestMessageMetaData metaData = new RequestMessageMetaData(destination, requestInfo);

                    RpcInvocation<IncomingMessageType> rpci =
                            new RpcInvocation<>(
                                    new IncomingMessage<>(incomingMessage, metaData),
                                    replyInfo -> {
                                        response.setCharacterEncoding("utf-8");
                                        try (NIOWriter out = response.getNIOWriter()) {
                                            response.setContentType("application/json");
                                            response.setContentLength(replyInfo._1.length());
                                            int statusCode;
                                            if (replyInfo._2 == null) {
                                                statusCode = 200;
                                            } else {
                                                replyInfo._2.headerParams.entrySet().forEach(
                                                        entry -> response.setHeader(entry.getKey(), entry.getValue())
                                                );
                                                statusCode = replyInfo._2.statusCode;
                                            }
                                            response.setStatus(statusCode);
                                            writeResponse(replyInfo._1, out);
                                            metricsCollector.requestCompleted(incomingMessage, replyInfo._1);
                                        } catch (Exception e) {
                                            metricsCollector.requestCompletedExceptionally(incomingMessage, e);
                                            logger.error("An error occurred trying to send HTTP response " + replyInfo, e);
                                            try {
                                                response.sendError(500, "Internal server error");
                                            } catch (Exception any) {
                                                logger.error("Failed to send error 500 back to client", any);
                                            }
                                        } finally {
                                            // TODO - this is from the example. Do we want to do something here?
                                            response.resume();
                                        }
                                    },
                                    error -> {
                                        logger.error("An error occurred trying to process HTTP request " + incomingMessageAsString, error);
                                        try {
                                            response.sendError(500, "Internal server error");
                                        } catch (Exception any) {
                                            logger.error("Failed to send error 500 back to client", any);
                                        }
                                    },
                                    messageTranscriber
                            );

                    metricsCollector.requestReceived(rpci.request);
                    stream.onNext(rpci);
                }
            });
        }
    }
}
