/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
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
