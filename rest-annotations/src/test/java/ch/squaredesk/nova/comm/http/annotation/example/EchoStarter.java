package ch.squaredesk.nova.comm.http.annotation.example;

import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class EchoStarter {
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(EchoConfiguration.class);
        ctx.refresh();

        // at this point, the REST server is started and properly initialized
        
        System.out.println("Echo server started");
        System.out.println("Press <ENTER> to shutdown");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line = reader.readLine();
        }
        System.out.println("Shutting down");
        ctx.getBean(HttpServer.class).shutdown();
    }
}
