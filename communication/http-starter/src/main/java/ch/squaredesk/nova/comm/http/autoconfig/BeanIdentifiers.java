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

package ch.squaredesk.nova.comm.http.autoconfig;

public interface BeanIdentifiers {
    String CLIENT = "httpClient";
    String SERVER = "httpServer";
    String MESSAGE_TRANSCRIBER = "httpMessageTranscriber";
    String OBJECT_MAPPER = "httpObjectMapper";
}
