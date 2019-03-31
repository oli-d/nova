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

public enum MediaType {
    APPLICATION_XML("application/xml"),
    APPLICATION_ATOM_XML("application/atom+xml"),
    APPLICATION_XHTML_XML("application/xhtml+xml"),
    APPLICATION_SVG_XML("application/atom+xml"),
    APPLICATION_JSON("application/json"),
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    APPLICATION_OCTET_STREAM("application/octet-stream"),
    TEXT_PLAIN("text/plain"),
    TEXT_XML("text/xml"),
    TEXT_HTML("text/html");

    public final String key;

    MediaType(String key) {
        this.key = key;
    }
}
