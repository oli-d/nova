/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

/**
 * Implementations of this class are used by the EventLoop to detect a duplicate event. If the Provider returns null,
 * the EventLoop processes the event normally. Only if a non null object is returned, the "duplication detection
 * framework" is triggered.
 */
public interface IdProviderForDuplicateEventDetection {
	Object provideIdFor(Object... data);
}
