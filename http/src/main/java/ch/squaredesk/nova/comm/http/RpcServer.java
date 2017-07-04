package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.io.NIOWriter;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RpcServer<InternalMessageType> extends ch.squaredesk.nova.comm.rpc.RpcServer<String, InternalMessageType, HttpSpecificInfo> {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private final MessageMarshaller<InternalMessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller;

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
    public <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
    Flowable<RpcInvocation<RequestType, ReplyType, HttpSpecificInfo>> requests(String destination, BackpressureStrategy backpressureStrategy) {
        // FIXME: handle multiple "subscriptions" to same path
        Subject<RpcInvocation<RequestType, ReplyType, HttpSpecificInfo>> rawSubject = PublishSubject.create();
        Subject<RpcInvocation<RequestType, ReplyType, HttpSpecificInfo>> subject = rawSubject.toSerialized();

        httpServer.getServerConfiguration().addHttpHandler(
                new HttpHandler() {
                    public void service(Request request, Response response) throws Exception {
                        RpcInvocation<RequestType, ReplyType, HttpSpecificInfo> rpci = new RpcInvocation<>(
                                (RequestType) readRequestObjectFrom(request, messageUnmarshaller),
                                httpSpecificInfoFrom(request),
                                reply -> {
                                    try {
                                        writeResponse(reply, response, messageMarshaller);
                                    } catch (Exception e) {
                                        // FIXME: write error or return Single.error()
                                        logger.error("An error occurred trying to write HTTP response", e);
                                    } finally {
                                        response.resume();
                                    }
                                },
                                error -> {
                                    logger.error("An error occurred trying to process HTTP request", error);
                                }
                        );

                        response.suspend();
                        subject.onNext(rpci);
                    }
                }, destination);

        return subject.toFlowable(backpressureStrategy);
    }

    private static HttpSpecificInfo httpSpecificInfoFrom (Request request) throws Exception {
        Map<String, String> parameters = new HashMap<>();
        for (Map.Entry<String,String[]> entry: request.getParameterMap().entrySet()) {
            String[] valueList = entry.getValue();
            String valueToPass = null;
            if (valueList != null && valueList.length>0) {
                valueToPass = valueList[0];
            }
            parameters.put(entry.getKey(), valueToPass);
        }

        return new HttpSpecificInfo(
                convert(request.getMethod()), parameters);
    }

    private static HttpRequestMethod convert (Method method) {
        if (method==Method.POST) {
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

    /**
     * synchronously reads request Object from request body
     */
    private static <T> T readRequestObjectFrom (Request request, MessageUnmarshaller<String,T> unmarshaller) throws Exception {
        String requestObjectAsString = "";
        try (BufferedReader br = new BufferedReader(request.getReader())) {
            requestObjectAsString = br.lines().collect(Collectors.joining("\n"));
        }
        return unmarshaller.unmarshal(requestObjectAsString);
    }

    /**
     * asynchronously writes reply Object to response body. Assumes that the marshaller creates a String that is a JSON
     * representation of the reply object
     */
    private static <ReplyType> void writeResponse (ReplyType reply, Response response, MessageMarshaller<ReplyType, String> marshaller) throws Exception {
        String responseAsString = marshaller.marshal(reply);
        NIOWriter out = response.getNIOWriter();
        BufferedWriter bw = new BufferedWriter(out);
        response.setContentType("application/json");
        response.setContentLength(responseAsString.length());
        bw.write(responseAsString);
        bw.flush();
        out.close();
        bw.close();
    }

    public void start() throws IOException {
        httpServer.start();
    }

    public void shutdown() {
        try {
            httpServer.shutdown(2, TimeUnit.SECONDS).get();
        } catch (Exception e) {
            logger.info("An error occurred, trying to shutdown REST HTTP server", e);
        }
    }
}
