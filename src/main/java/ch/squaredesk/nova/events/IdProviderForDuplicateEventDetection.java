/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events;

/**
 * Implementations of this class are used by the EventEmitter to detect a duplicate event. If the Provider returns null,
 * the EventEmitter processes the event normally. Only if a non null object is returned, the "duplication detection
 * framework" is triggered.
 */
public interface IdProviderForDuplicateEventDetection {
	Object provideIdFor(Object... data);
}
