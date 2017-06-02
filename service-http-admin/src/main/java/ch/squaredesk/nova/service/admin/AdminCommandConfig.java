/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.service.admin;

import java.lang.reflect.Method;

public class AdminCommandConfig {
    public final Object objectToInvokeMethodOn;
    public final Method methodToInvoke;
    public final String[] parameterNames;

    public AdminCommandConfig(Object objectToInvokeMethodOn, Method methodToInvoke, String... parameterNames) {
        this.objectToInvokeMethodOn = objectToInvokeMethodOn;
        this.methodToInvoke = methodToInvoke;
        this.parameterNames = parameterNames;
    }
}
