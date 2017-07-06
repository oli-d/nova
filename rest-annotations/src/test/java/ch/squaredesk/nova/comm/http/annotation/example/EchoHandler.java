package ch.squaredesk.nova.comm.http.annotation.example;

import ch.squaredesk.nova.comm.http.HttpRequestMethod;
import ch.squaredesk.nova.comm.http.MediaType;
import ch.squaredesk.nova.comm.http.annotation.OnRestRequest;

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
