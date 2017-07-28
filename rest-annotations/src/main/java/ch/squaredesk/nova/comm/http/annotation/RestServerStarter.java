package ch.squaredesk.nova.comm.http.annotation;

import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.PreDestroy;
import java.io.IOException;

public class RestServerStarter implements ApplicationListener<ContextRefreshedEvent> {
    private Logger logger = LoggerFactory.getLogger(RestServerStarter.class);

    private HttpServer httpServer;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        httpServer = event.getApplicationContext().getBean(HttpServer.class);

        if (!httpServer.isStarted()) {
            try {
                httpServer.start();
            } catch (IOException e) {
                logger.error("Unable to start HttpServer with configuration " +
                        event.getApplicationContext().getBean(HttpServerConfiguration.class), e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        if (httpServer != null) {
            httpServer.shutdown();
        }
    }

}
