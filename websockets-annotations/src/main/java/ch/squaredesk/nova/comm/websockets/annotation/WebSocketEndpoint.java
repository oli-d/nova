/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets.annotation;

import java.lang.reflect.Method;

public class WebSocketEndpoint {
    public final RestResourceDescriptor resourceDescriptor;
    public final Method handlerMethod;

    public WebSocketEndpoint(RestResourceDescriptor resourceDescriptor, Method handlerMethod) {
        this.resourceDescriptor = resourceDescriptor;
        this.handlerMethod = handlerMethod;
    }
}
