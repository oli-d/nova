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

package ch.squaredesk.nova.autoconfigure.service;

/**
 * Tag interface. If we find a single bean implementing this interface,
 * a {@link ServiceDescriptor} instance will be available in the application context.
 */
public interface NovaService {
}
