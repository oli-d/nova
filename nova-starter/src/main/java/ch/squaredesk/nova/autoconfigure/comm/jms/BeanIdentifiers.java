/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.jms;

public interface BeanIdentifiers {
    String MESSAGE_TRANSCRIBER = "jmsMessageTranscriber";
    String OBJECT_MAPPER = "jmsObjectMapper";
    String CORRELATION_ID_GENERATOR = "jmsCorrelationIdGenerator";
    String DESTINATION_ID_GENERATOR = "jmsDestinationIdGenerator";
}
