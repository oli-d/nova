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

package ch.squaredesk.nova.metrics;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class MetricsName {
    public static String buildName(String ... nameParts) {
        if (nameParts == null || nameParts.length == 0) {
            return "";
        }

        return Arrays.stream(nameParts)
                .filter(part -> !Objects.isNull(part) && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining("."));
    }
}
