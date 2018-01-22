/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.rest.annotation;

import ch.squaredesk.nova.comm.http.HttpRequestMethod;
import ch.squaredesk.nova.comm.http.MediaType;
import org.glassfish.jersey.server.model.Resource;

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
