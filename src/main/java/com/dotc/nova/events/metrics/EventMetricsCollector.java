package com.dotc.nova.events.metrics;

public interface EventMetricsCollector {
	void eventDispatched(Object event);

	void errorOccurredForEventDispatch(Object event);

	void duplicateEventDetected(Object event);

	void eventDroppedBecauseOfFullQueue(Object event);

	void eventAddedToFullQueue(Object event);

	void eventAddedToDispatchLaterQueue(Object event);

	void listenerAdded(Object event);

	void listenerRemoved(Object event);

	void allListenersRemoved(Object event);

	void eventEmittedButNoListeners(Object event);
}
