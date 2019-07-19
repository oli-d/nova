package ch.squaredesk.nova.comm.http;

import java.net.URL;
import java.util.Optional;

public class MetricsCollectorInfoCreator {

    public static String createInfoFor(URL url) {
        return Optional.ofNullable(url)
                .map(u -> {
                    String path = u.getPath().replaceAll("/", ".");
                    if (path.startsWith(".")) {
                        return path.substring(1);
                    } else {
                        return path;
                    }
                })
                .orElse(null);
    }


}
