/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

public class ReplyInfo {
    public final int statusCode;

    public ReplyInfo(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("{\n")
            .append("\tstatusCode: ").append(statusCode).append('\n')
            .append("}")
            .toString();
    }
}
