package ch.squaredesk.nova.service.admin;

import static java.util.Objects.requireNonNull;

public class AdminUrlCalculator {
    private final String hostName;
    private final String baseUrl;
    private final int port;


    public AdminUrlCalculator(String hostName, String baseUrl, int port) {
        requireNonNull(hostName, "hostName must not be null");
        requireNonNull(hostName, "baseUrl must not be null");
        this.hostName = hostName;
        if (baseUrl.startsWith("/")) {
            this.baseUrl = baseUrl;
        } else {
            this.baseUrl = "/" + baseUrl;
        }
        this.port = port;
    }

    String urlFor (AdminCommandConfig acc) {
        requireNonNull(acc, "config must not be null");

        return "rest://" + hostName + ":" + port + baseUrl + "/" +
                acc.methodToInvoke.getName();
    }
}
