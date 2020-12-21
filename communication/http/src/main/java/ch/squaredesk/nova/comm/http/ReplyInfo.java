/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;

import java.util.Collections;
import java.util.Map;

public class ReplyInfo {
    public final int statusCode;
    public final Map<String, String> headerParams;

    public ReplyInfo(int statusCode) {
        this(statusCode , null);
    }

    public ReplyInfo(int statusCode, Map<String, String> headerParams) {
        this.statusCode = statusCode;
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
            .append("\tstatusCode: ").append(statusCode).append('\n')
            .append("\theaders: {\n");
        for (Map.Entry<String, String> entry: headerParams.entrySet()) {
            sb.append("\t\t").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        sb.append("\t}\n");
        sb.append("}");
        return sb.toString();
    }
}
