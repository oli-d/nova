/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.rest.autoconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
public class RestInvocationLogger implements ContainerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RestInvocationLogger.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        logger.debug("Invoking REST handler for path {} ", requestContext.getUriInfo().getPath(true));
        logger.debug("Request headers:");
        requestContext.getHeaders().forEach((key, values) -> logger.debug("\t{}: {}", key, values));
    }
}
