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

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Path("/echo")
public class EchoHandler {
    @GET
    public String echoParameterValue(@QueryParam("p1") String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }

    @GET
    @Path("/{text}")
    public String echoPathValue(@PathParam("text") String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }

    @GET
    @Path("/async/{text}")
    public void echoPathValueAsync(@Context HttpHeaders headers, @PathParam("text") String textFromCallerToEcho, @Suspended AsyncResponse response) {
        System.out.println("Headers: ");
        headers.getRequestHeaders().forEach((key, val) -> System.out.println(key + "=>" + val));
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Response res = Response.status(555).header("myHeader", "myHeaderValue").entity(textFromCallerToEcho).build();
            response.resume(res);
        }).start();
    }

    @POST
    public String echoPostRequestBody(String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }

}
