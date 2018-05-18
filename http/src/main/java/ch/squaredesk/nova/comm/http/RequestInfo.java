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

public class RequestInfo {
    public final HttpRequestMethod requestMethod;
    public final Map<String, String> headerParams;

    public RequestInfo(HttpRequestMethod requestMethod) {
        this(requestMethod, null);
    }

    public RequestInfo(HttpRequestMethod requestMethod, Map<String, String> headerParams) {
        this.requestMethod = requestMethod;
        if (headerParams == null) {
            this.headerParams = Collections.emptyMap();
        } else {
            this.headerParams = headerParams;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append("{\n")
            .append("\trequestMethod: ").append(requestMethod).append('\n')
            .append("\theaderParams: {\n");
        for (Map.Entry<String, String> entry: headerParams.entrySet()) {
            sb.append("\t\t").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        sb.append("\t}\n");
        sb.append("}");
        return sb.toString();
    }
}
