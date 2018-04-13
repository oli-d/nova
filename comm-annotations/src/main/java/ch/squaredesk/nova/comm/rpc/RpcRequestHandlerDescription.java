/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.rpc;

import java.lang.reflect.Method;

public class RpcRequestHandlerDescription {
    public final Class<?> requestClass;
    public final Class<?> replyClass;
    public final Object bean;
    public final Method methodToInvoke;

    public RpcRequestHandlerDescription(Class<?> requestClass, Class<?> replyClass, Object bean, Method methodToInvoke) {
        this.requestClass = requestClass;
        this.replyClass = replyClass;
        this.bean = bean;
        this.methodToInvoke = methodToInvoke;
    }
}
