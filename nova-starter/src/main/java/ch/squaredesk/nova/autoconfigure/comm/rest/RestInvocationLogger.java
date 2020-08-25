/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

@Provider
@Priority(Priorities.USER)
public class RestInvocationLogger implements ContainerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RestInvocationLogger.class);

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method method = resourceInfo.getResourceMethod();
        Optional<InvocationLog> annotation = Optional.ofNullable(method).map(m -> m.getAnnotation(InvocationLog.class));
        boolean logEnabled = annotation.map(InvocationLog::enabled).orElse(true);

        if (!logEnabled) {
            return;
        }

        boolean logHeaders = annotation.map(InvocationLog::logHeaders).orElse(true);
        String logLevel = annotation.map(InvocationLog::logLevel).orElse("DEBUG");
        Consumer<String> loggerMethod = null;
        switch (logLevel) {
            case "ERROR" :
                loggerMethod = logger::error;
                break;
            case "INFO" :
                loggerMethod = logger::info;
                break;
            case "TRACE" :
                loggerMethod = logger::trace;
                break;
            default:
                loggerMethod = logger::debug;
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Invoking ").append(requestContext.getRequest().getMethod()).append(" handler for path ").append(requestContext.getUriInfo().getRequestUri()).append("\n");

        if (logHeaders) {
            stringBuilder.append("Request headers:\n");
            requestContext.getHeaders().forEach((key, values) -> stringBuilder.append("\t").append(key).append(": ").append(values).append("\n"));
        }

        loggerMethod.accept(stringBuilder.toString());
    }
}
