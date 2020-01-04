/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

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
