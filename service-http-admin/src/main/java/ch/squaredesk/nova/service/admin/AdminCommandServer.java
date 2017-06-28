package ch.squaredesk.nova.service.admin;

import ch.squaredesk.nova.comm.rest.HttpCommAdapter;
import ch.squaredesk.nova.service.admin.messages.AdminMessage;
import ch.squaredesk.nova.service.admin.messages.SupportedCommand;
import ch.squaredesk.nova.tuples.Pair;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.Disposable;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AdminCommandServer implements ApplicationListener<ContextRefreshedEvent> {
    private final Logger logger = LoggerFactory.getLogger(AdminCommandServer.class);

    private final AdminInfoCenter adminInfoCenter;
    private HttpServer httpServer;
    private ObjectMapper objectMapper;

    private boolean alreadyStarted = false;
    private final Map<String, Disposable> supportedUrls = new HashMap<>();

    public AdminCommandServer(AdminInfoCenter adminInfoCenter) {
        this.adminInfoCenter = adminInfoCenter;
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext ctx = event.getApplicationContext();
        commAdapter = (HttpCommAdapter<AdminMessage>) ctx.getBean("adminCommAdapter");
        objectMapper = ctx.getBean("adminObjectMapper", ObjectMapper.class);
        String baseUrl = ctx.getBean("adminBaseUrl", String.class);

        for (Pair<AdminCommandConfig, SupportedCommand> config: adminInfoCenter.getConfigs()) {
            String relativeUrl = calculateRelativeUrl(baseUrl, config._2.url);
            if (supportedUrls.containsKey(relativeUrl)) {
                logger.info("   Not registering URL " + relativeUrl +  " a second time...");
                continue;
            }

            AdminCommandMethodInvoker mi = new AdminCommandMethodInvoker(config._1);
            Disposable subscription = commAdapter.requests(relativeUrl, BackpressureStrategy.BUFFER).subscribe(mi::accept);
            supportedUrls.put(relativeUrl, subscription);
            logger.info("Supporting URL " + relativeUrl);
        }

        if (!alreadyStarted) {
            try {
                commAdapter.start();
                SupportedCommand infoCommand = Arrays.stream(adminInfoCenter.getInfo().supportedCommands)
                        .filter(sc -> sc.url.endsWith("/info"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("unable to register info URL"));

                logger.info("Successfully started server to listen to admin HTTP requests. For supported commands visit " +
                        infoCommand.url);
            } catch (Exception e) {
                throw new RuntimeException("Unable to start admin server", e);
            }
            alreadyStarted = true;
        }
    }

    static String calculateRelativeUrl (String baseUrl, String absoluteUrl) {
        int idx = absoluteUrl.indexOf(baseUrl);
        if (idx<0) return absoluteUrl;
        else return absoluteUrl.substring(idx);
    }

    // FIXME: call it. Shutdown hook???
    public void shutdown() {
        try {
            httpServer.shutdown(2, TimeUnit.SECONDS).get();
        } catch (Exception e) {
            logger.error("Unable to shutdown admin HTTP server", e);
        }
        for (Disposable d: supportedUrls.values()) d.dispose();
        supportedUrls.clear();
        logger.info("AdminCommand HTTP server successfully shut down");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/admin/info")
    public String info() {
        try {
            return objectMapper.writeValueAsString(adminInfoCenter.getInfo());
        } catch (Exception e) {
            logger.error("Unable to return info ", e);
            return "Unable to serialize info " + e.getLocalizedMessage();
        }
    }
}