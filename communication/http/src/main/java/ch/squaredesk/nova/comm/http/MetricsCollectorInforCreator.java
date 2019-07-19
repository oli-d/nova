package ch.squaredesk.nova.comm.http;

import java.net.URL;
import java.util.Optional;

public class MetricsCollectorInforCreator {

    public static String createInfoFor(URL url) {
        return Optional.ofNullable(url)
                .map(u -> u.getPath().replaceAll("/", "."))
                .orElse(null);
    }

}
