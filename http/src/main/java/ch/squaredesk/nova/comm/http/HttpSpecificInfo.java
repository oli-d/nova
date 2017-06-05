/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;

import java.util.Collections;
import java.util.Map;

public class HttpSpecificInfo {
    public final HttpRequestMethod requestMethod;
    public final Map<String, String> parameters;

    public HttpSpecificInfo(HttpRequestMethod requestMethod) {
        this(requestMethod, null);
    }

    public HttpSpecificInfo(HttpRequestMethod requestMethod, Map<String, String> parameters) {
        this.requestMethod = requestMethod;
        if (parameters == null) this.parameters = Collections.emptyMap();
        else this.parameters = parameters;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append("{\n")
            .append("\trequestMethod: ").append(requestMethod).append('\n')
            .append("\tparameters: {\n");
        for (Map.Entry<String, String> entry: parameters.entrySet()) {
            sb.append("\t\t").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        sb.append("\t}\n");
        sb.append("}");
        return sb.toString();
    }
}
