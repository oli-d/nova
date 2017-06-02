package ch.squaredesk.nova.service.admin;

import ch.squaredesk.nova.comm.http.HttpCommAdapter;
import ch.squaredesk.nova.service.admin.messages.AdminMessage;
import ch.squaredesk.nova.service.admin.messages.SupportedCommand;
import io.reactivex.BackpressureStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AdminCommandServer implements ApplicationListener<ContextRefreshedEvent> {
    private final Logger logger = LoggerFactory.getLogger(AdminCommandServer.class);
    private final AdminInfoCenter adminInfoCenter;

    private boolean alreadyStarted = false;
    private final Set<String> supportedUrls = new HashSet<>();

    public AdminCommandServer(AdminInfoCenter adminInfoCenter) {
        this.adminInfoCenter = adminInfoCenter;
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext ctx = event.getApplicationContext();
        HttpCommAdapter<AdminMessage> commAdapter = (HttpCommAdapter<AdminMessage>) ctx.getBean("adminCommAdapter");
        String baseUrl = ctx.getBean("adminBaseUrl", String.class);

        if (!alreadyStarted) {
            try {
                SupportedCommand infoCommand = Arrays.stream(adminInfoCenter.getInfo().supportedCommands)
                        .filter(sc -> sc.url.endsWith("/info"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("unable to register info URL"));

                logger.info("Starting server to listen to admin HTTP requests. For supported commands visit " +
                        infoCommand.url);
            } catch (Exception e) {
                throw new RuntimeException("Unable to start admin server", e);
            }
            alreadyStarted = true;
        }

        for (AdminCommandConfig acc: adminInfoCenter.getConfigs()) {
            String relativeUrl = calculateRelativeUrl(baseUrl, acc.url);
            if (supportedUrls.contains(relativeUrl)) {
                logger.info("   Not registering URL " + relativeUrl +  " a second time...");
                continue;;
            }

            System.out.println("Supporting URL " + relativeUrl);

            commAdapter.requests(relativeUrl, BackpressureStrategy.BUFFER).subscribe(
                    rpcInvocation -> {
                        rpcInvocation.request
                    }
            );
        }
    }

    private static String calculateRelativeUrl (String baseUrl, String absoluteUrl) {
        int idx = absoluteUrl.indexOf(baseUrl);
        if (idx<0) return absoluteUrl;
        else return absoluteUrl.substring(idx + baseUrl.length());
    }

    @OnAdminCommand()
    public String info() {
        return adminInfoCenter.getInfo().toString();
    }
}