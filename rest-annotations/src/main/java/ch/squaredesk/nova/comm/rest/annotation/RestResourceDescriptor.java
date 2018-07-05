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

import static ch.squaredesk.nova.comm.http.MediaType.APPLICATION_JSON;

public class RestResourceDescriptor {
    public final String path;
    public final MediaType produces;
    public final MediaType[] consumes;
    public final HttpRequestMethod requestMethod;

    private RestResourceDescriptor(String path, MediaType produces, MediaType[] consumes, HttpRequestMethod requestMethod) {
        this.path = path;
        this.produces = produces;
        this.consumes = consumes;
        this.requestMethod = requestMethod;
    }

    public static RestResourceDescriptor from (String path) {
        return new RestResourceDescriptor(path, APPLICATION_JSON, new MediaType[] { APPLICATION_JSON}, HttpRequestMethod.POST );
    }

    public static RestResourceDescriptor from (String path, HttpRequestMethod httpRequestMethod) {
        return new RestResourceDescriptor(path, APPLICATION_JSON, new MediaType[] { APPLICATION_JSON}, httpRequestMethod );
    }

    public static RestResourceDescriptor from (String path, HttpRequestMethod httpRequestMethod, MediaType produces) {
        return new RestResourceDescriptor(path, produces, new MediaType[] { APPLICATION_JSON}, httpRequestMethod );
    }

    public static RestResourceDescriptor from (String path, HttpRequestMethod httpRequestMethod, MediaType produces, MediaType ... consumes) {
        return new RestResourceDescriptor(path, produces, consumes, httpRequestMethod );
    }
}
