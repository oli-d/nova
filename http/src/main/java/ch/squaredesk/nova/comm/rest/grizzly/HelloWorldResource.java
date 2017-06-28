package ch.squaredesk.nova.comm.rest.grizzly;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class HelloWorldResource {

    @GET
    @Produces("text/plain")
    @Path("helloworld")
    public String helloWorld() {
        return "Hello World!";
    }

    @GET
    @Path("/spring1")
    @Produces(MediaType.TEXT_PLAIN)
    public String singleParamMethod() throws Exception {
        return "Success1";
    }

}