package ch.squaredesk.nova.comm.rest;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Resource;

import javax.ws.rs.container.ContainerRequestContext;
import java.lang.reflect.Method;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public class RestResourceFactory {
    public static Resource resourceFor(RestResourceDescriptor resourceDescriptor, Object handlerBean, Method handlerMethod) {
        requireNonNull(resourceDescriptor, "resourceDescriptor must not be null");
        requireNonNull(handlerBean, "handlerBean must not be null");
        requireNonNull(handlerMethod, "handlerMethod must not be null");

        Resource.Builder resourceBuilder = Resource.builder("/");
        resourceBuilder
                .path(resourceDescriptor.path)
                .addMethod(convert(resourceDescriptor.requestMethod))
                .produces(convert(resourceDescriptor.produces))
                .consumes(convert(resourceDescriptor.consumes))
                .handledBy(handlerBean, handlerMethod);
        return resourceBuilder.build();
    }

    public static Resource resourceFor(RestResourceDescriptor resourceDescriptor, Inflector<ContainerRequestContext, ?> inflector) {
        requireNonNull(resourceDescriptor, "resourceDescriptor must not be null");
        requireNonNull(inflector, "inflector must not be null");

        Resource.Builder resourceBuilder = Resource.builder("/");
        resourceBuilder
                .path(resourceDescriptor.path)
                .addMethod(convert(resourceDescriptor.requestMethod))
                .produces(convert(resourceDescriptor.produces))
                .consumes(convert(resourceDescriptor.consumes))
                .handledBy(inflector);
        return resourceBuilder.build();
    }

    private static String convert (HttpRequestMethod requestMethod) {
        return String.valueOf(requestMethod);
    }

    private static String convert (MediaType mediaType) {
        return mediaType.key;
    }

    private static String[] convert (MediaType... mediaTypes) {
        return Arrays.stream(mediaTypes)
                .map(RestResourceFactory::convert)
                .toArray(String[]::new);
    }
}
