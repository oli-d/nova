package com.dotc.nova.events.metrics;

public class NoopEventMetricsCollector implements EventMetricsCollector {

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
	public void errorOccurredForEventDispatch(Object event) {
	}

}
