/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events.metrics;


public class NoopEventMetricsCollector extends EventMetricsCollector {

	public NoopEventMetricsCollector() {
		super(null, null);
	}

	@Override
	public void eventDispatched(Object event) {
	}

	@Override
	public void duplicateEventDetected(Object event) {
	}

	@Override
	public void eventDroppedBecauseOfFullQueue(Object event) {
	}

	@Override
	public void eventAddedToFullQueue(Object event) {
	}

	@Override
	public void eventAddedToDispatchLaterQueue(Object event) {
	}

	@Override
	public void listenerAdded(Object event) {
	}

	@Override
	public void listenerRemoved(Object event) {
	}

	@Override
	public void allListenersRemoved(Object event) {
	}

	@Override
	public void eventEmittedButNoListeners(Object event) {
	}

	@Override
	public void waitedForEventToBeDispatched(Object event, long waitTime) {

	}
}
