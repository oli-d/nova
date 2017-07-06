package ch.squaredesk.nova.comm.http.annotation.example;

import ch.squaredesk.nova.comm.http.HttpRequestMethod;
import ch.squaredesk.nova.comm.http.MediaType;
import ch.squaredesk.nova.comm.http.annotation.OnRestRequest;

public class EchoHandler {
    @OnRestRequest(value = "/echo", requestMethod = HttpRequestMethod.GET, consumes = MediaType.TEXT_PLAIN)
    public String echo(String p1) {
        System.out.println("I am here! " + p1);
        return p1;
    }
}
