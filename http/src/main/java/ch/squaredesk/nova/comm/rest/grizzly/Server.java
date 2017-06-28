package ch.squaredesk.nova.comm.rest.grizzly;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;


public class Server {

    private final URI ADDRESS = UriBuilder.fromPath("rest://localhost:8888").build();

    public Server() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("ch.squaredesk.nova.comm.rest.grizzly");

//        resourceBuilder.addChildResource("info")
//        Resource.Builder resourceBuilder = Resource.builder("/");
//        resourceBuilder
//                .addMethod("GET")
//                .produces("text/plain")
//                .handledBy(requestContext -> "Let's do some lambda administration!");
//        resourceBuilder.addChildResource("[^/]+?")
//                .addMethod("GET")
//                .produces("text/plain")
//                .handledBy(requestContext -> "Let's do some generic administration!");
//        resourceConfig.registerResources(resourceBuilder.build());

        HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(ADDRESS, resourceConfig);
        try {
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        System.out.println("" + new Date() + " - starting");
        Server server = new Server();
        System.out.println("" + new Date() + " - started :-)");
    }

    /*

Have a look at the example provided by the documentation:

@Path("hello")
public class HelloResource {

     @GET
     @Produces("text/plain")
     public String sayHello() {
         return "Hello!";
     }
}
// Register the annotated resource.
ResourceConfig resourceConfig = new ResourceConfig(HelloResource.class);

// Add new "hello2" resource using the annotated resource class
// and overriding the resource path.
Resource.Builder resourceBuilder =
        Resource.builder(HelloResource.class, new LinkedList<ResourceModelIssue>())
        .path("hello2");

// Add a new (virtual) sub-resource method to the "hello2" resource.
resourceBuilder.addChildResource("world")
        .addMethod("GET")
        .produces("text/plain")
        .handledBy(new Inflector<Request, String>() {

                @Override
                public String apply(Request request) {
                    return "Hello World!";
                }
        });

// Register the new programmatic resource in the application's configuration.
resourceConfig.registerResources(resourceBuilder.build());
The following table illustrates the supported requests and provided responses for the application configured in the example above:

  Request              |  Response        |  Method invoked
-----------------------+------------------+----------------------------
   GET /hello          |  "Hello!"        |  HelloResource.sayHello()
   GET /hello2         |  "Hello!"        |  HelloResource.sayHello()
   GET /hello2/world   |  "Hello World!"  |  Inflector.apply()
For additional details, check the Jersey documentation.

     */
}