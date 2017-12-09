/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.rest.annotation.example;

import ch.squaredesk.nova.comm.http.HttpRequestMethod;
import ch.squaredesk.nova.comm.rest.annotation.OnRestRequest;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

public class EchoHandler {
    @OnRestRequest("/echo")
    public String echoParameterValue(@QueryParam("p1") String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }

    @OnRestRequest("/echo/{text}")
    public String echoPathValue(@PathParam("text") String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }

    @OnRestRequest(value = "/echo", requestMethod = HttpRequestMethod.POST)
    public String echoRequestObject(String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }
}
