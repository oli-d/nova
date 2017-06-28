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

import ch.squaredesk.nova.comm.rest.HttpRequestMethod;

import java.lang.reflect.Method;

public class RestEndpointDescription {
    public final String path;
    public final MediaType produces;
    public final MediaType[] consumes;
    public final HttpRequestMethod requestMethod;
    public final Method handlerMethod;

    public RestEndpointDescription(String path, MediaType produces, MediaType[] consumes, HttpRequestMethod requestMethod, Method handlerMethod) {
        this.path = path;
        this.produces = produces;
        this.consumes = consumes;
        this.requestMethod = requestMethod;
        this.handlerMethod = handlerMethod;
    }
}
