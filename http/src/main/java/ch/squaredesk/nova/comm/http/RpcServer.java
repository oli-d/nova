package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RpcServer<InternalMessageType> extends ch.squaredesk.nova.comm.rpc.RpcServer<String, RpcInvocation<InternalMessageType>> {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private final MessageMarshaller<InternalMessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller;
    private final Map<String, Flowable<RpcInvocation<? extends InternalMessageType>>> mapDestinationToIncomingMessages = new ConcurrentHashMap<>();

    private final HttpServer httpServer;

    protected RpcServer(HttpServer httpServer,
                        MessageMarshaller<InternalMessageType, String> messageMarshaller,
                        MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                        Metrics metrics) {
        this(null, httpServer, messageMarshaller, messageUnmarshaller, metrics);
    }

    protected RpcServer(String identifier,
                        HttpServer httpServer,
                        MessageMarshaller<InternalMessageType, String> messageMarshaller,
                        MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                        Metrics metrics) {
        super(identifier, metrics);
        Objects.requireNonNull(httpServer, "httpServer must not be null");
        Objects.requireNonNull(messageMarshaller, "messageMarshaller must not be null");
        Objects.requireNonNull(messageUnmarshaller, "messageUnmarshaller must not be null");
        this.httpServer = httpServer;
        this.messageUnmarshaller = messageUnmarshaller;
        this.messageMarshaller = messageMarshaller;
    }

    @Override
    public Flowable<RpcInvocation<InternalMessageType>> requests(String destination) {
        Flowable retVal = mapDestinationToIncomingMessages
                .computeIfAbsent(destination, key -> {
                    logger.info("Listening to requests on " + destination);

                    Subject<RpcInvocation<? extends InternalMessageType>> stream = PublishSubject.create();
                    stream = stream.toSerialized();
                    NonBlockingHttpHandler httpHandler = new NonBlockingHttpHandler(stream);

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

    private static SendingInfo httpSpecificInfoFrom(Request request) throws Exception {
        Map<String, String> parameters = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] valueList = entry.getValue();
            String valueToPass = null;
            if (valueList != null && valueList.length > 0) {
                valueToPass = valueList[0];
            }
            parameters.put(entry.getKey(), valueToPass);
        }

        return new SendingInfo(
                convert(request.getMethod()), parameters);
    }

    private static HttpRequestMethod convert(Method method) {
        if (method == Method.POST) {
            return HttpRequestMethod.POST;
        } else if (method == Method.DELETE) {
            return HttpRequestMethod.DELETE;
        } else if (method == Method.PUT) {
            return HttpRequestMethod.PUT;
        } else {
            // TODO: do we want to add all other ones known to grizzly?
            return HttpRequestMethod.GET;
        }
    }

    private static <T> T convertRequestData(String objectAsString, MessageUnmarshaller<String, T> unmarshaller) throws Exception {
        return unmarshaller.unmarshal(objectAsString);
    }

    private static <T> String convertResponseData(T replyObject, MessageMarshaller<T, String> marshaller) throws Exception {
        return marshaller.marshal(replyObject);
    }

    /**
     * writes reply Object to response body. Assumes that the marshaller creates a String that is a JSON
     * representation of the reply object
     */
    private static void writeResponse(String reply, NIOWriter out) throws Exception {
        out.write(reply);
        out.flush();
        out.close();
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


    /**
     * This class implements the non-blocking http handler. It takes the incoming requests and puts them into
     * a blocking(!) queue to be processed by interested consumers.
     * <p>
     * Blocking? Yes, because this way we apply backpressure and only read those messages from the wire that we are
     * able to process. The size of the queue defines, how many requests we are willing to lose in the worst case
     */
    private class NonBlockingHttpHandler extends HttpHandler {
        private final Subject<RpcInvocation<? extends InternalMessageType>> stream;

        private NonBlockingHttpHandler(
                Subject<RpcInvocation<? extends InternalMessageType>> stream) {
            this.stream = stream;
        }

        public void service(Request request, Response response) throws Exception {
            response.suspend();

            NIOWriter out = response.getNIOWriter();
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
                    String requestAsString = new String(inputBuffer);
                    try {
                        in.close();
                    } catch (Exception ignored) {
                        // TODO - this is from the example. Do we want to do something here?
                    }

                    InternalMessageType requestObject = convertRequestData(new String(inputBuffer), messageUnmarshaller);
                    RpcInvocation<? extends InternalMessageType> rpci =
                            new RpcInvocation<>(
                                    requestObject,
                                    httpSpecificInfoFrom(request),
                                    replyInfo -> {
                                        try {
                                            String responseAsString = convertResponseData(replyInfo._1, messageMarshaller);
                                            response.setContentType("application/json");
                                            response.setContentLength(responseAsString.length());
                                            int statusCode = replyInfo._2 == null ? 200 : replyInfo._2.statusCode;
                                            response.setStatus(statusCode);
                                            writeResponse(responseAsString, out);
                                            metricsCollector.requestCompleted(requestObject, responseAsString);
                                        } catch (Exception e) {
                                            metricsCollector.requestCompletedExceptionally(requestObject, e);
                                            logger.error("An error occurred trying to send HTTP response " + replyInfo, e);
                                            try {
                                                response.sendError(500, "Internal server error");
                                            } catch (Exception any) {
                                                logger.error("Failed to send error 500 back to client", any);
                                            }
                                        } finally {
                                            try {
                                                out.close();
                                            } catch (Exception ignored) {
                                                // TODO - this is from the example. Do we want to do something here?
                                            }
                                            response.resume();
                                        }
                                    },
                                    error -> {
                                        logger.error("An error occurred trying to process HTTP request " + requestAsString, error);
                                        try {
                                            response.sendError(500, "Internal server error");
                                        } catch (Exception any) {
                                            logger.error("Failed to send error 500 back to client", any);
                                        }
                                    }
                            );

                    metricsCollector.requestReceived(rpci.request);
                    stream.onNext(rpci);
                }
            });
        }
    }
}
