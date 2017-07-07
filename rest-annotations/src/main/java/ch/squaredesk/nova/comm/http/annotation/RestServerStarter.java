package ch.squaredesk.nova.comm.http.annotation;

import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.io.IOException;

public class RestServerStarter implements ApplicationListener {
    private Logger logger = LoggerFactory.getLogger(RestServerStarter.class);

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            ApplicationContext ctx = ((ContextRefreshedEvent) event).getApplicationContext();
            HttpServer httpServer = ctx.getBean(HttpServer.class);

            if (!httpServer.isStarted()) {
                try {
                    httpServer.start();
                } catch (IOException e) {
                    logger.error("Unable to start HttpServer with configuration " + ctx.getBean(HttpServerConfiguration.class) , e);
                }
            }
        }
    }
}
