package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.comm.rpc.RpcServer;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RestServer<InternalMessageType> extends RpcServer<String, InternalMessageType, HttpSpecificInfo> {
    private final Logger logger = LoggerFactory.getLogger(RestServer.class);

    private final RestServerConfiguration serverConfiguration;
    private final ResourceConfig resourceConfig = new ResourceConfig();
    private HttpServer httpServer;

    protected RestServer(RestServerConfiguration config, Metrics metrics) {
        this(null, config, metrics);
    }

    protected RestServer(String identifier, RestServerConfiguration config, Metrics metrics) {
        super(identifier, metrics);
        Objects.requireNonNull(config, "restServerConfiguration must not be null");
        this.serverConfiguration = config;
    }

    @Override
    public <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
    Flowable<RpcInvocation<RequestType, ReplyType, HttpSpecificInfo>> requests(String destination, BackpressureStrategy backpressureStrategy) {
        if (httpServer!=null && httpServer.isStarted()) {
            throw new IllegalStateException("Unable to register requests after server has been started");
        }

        // FIXME: must be configured by the caller
        RestResourceDescriptor rrd = RestResourceDescriptor.from(destination, HttpRequestMethod.GET);
        // FIXME:
        Object bean = new Object();
        Method method = null;
        try {
            method = Object.class.getDeclaredMethod("toString");
        } catch (Exception e) {
            // noop
        }
        resourceConfig.registerResources(RestResourceFactory.resourceFor(rrd, requestContainer -> "Hallo"));

        return Flowable.empty();
    }



    public void start() {
        httpServer = HttpServerFactory.serverFor(serverConfiguration, resourceConfig);
    }

    public void shutdown() {
        try {
            httpServer.shutdown(2, TimeUnit.SECONDS).get();
        } catch (Exception e) {
            logger.info("An error occurred, trying to shutdown REST HTTP server", e);
        }
    }
}
