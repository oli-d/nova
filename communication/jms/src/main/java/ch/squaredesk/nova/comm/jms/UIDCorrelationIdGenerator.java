/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;


import java.util.UUID;

public class UIDCorrelationIdGenerator implements java.util.function.Supplier<String> {
    @Override
    public String get() {
        return UUID.randomUUID().toString();
    }
}
