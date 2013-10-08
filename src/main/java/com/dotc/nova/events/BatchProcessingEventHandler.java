package com.dotc.nova.events;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

class BatchProcessingEventHandler implements EventHandler<InvocationContext> {
	private final static Logger LOGGER = LoggerFactory.getLogger(BatchProcessingEventHandler.class);

	private static final int QUEUE_SIZE_WARNING_THRESHOLD = 1000;

	private final Map<Object, Object[]> batchedEventDataMap = new HashMap<>();
	private final Map<Object, Set<EventListener>> batchedEventListenerMap = new HashMap<>();
	private final List<InvocationContext> orderedEventList = new ArrayList<InvocationContext>();
	private boolean inBatch = false;

	private void storeForLaterProcessing(InvocationContext invocationContext) {
		int queueSize = orderedEventList.size();
		if (queueSize > QUEUE_SIZE_WARNING_THRESHOLD && queueSize % 10 == 0) {
			LOGGER.warn("Event queue size is now " + queueSize);
		}

		Object event = invocationContext.getEvent();
		// register listener
		Set<EventListener> eventListeners = batchedEventListenerMap.get(event);
		if (eventListeners == null) {
			eventListeners = new HashSet<>();
			batchedEventListenerMap.put(event, eventListeners);
		}
		EventListener currentEventListener = invocationContext.getEventListener();
		if (!eventListeners.contains(currentEventListener)) {
			eventListeners.add(currentEventListener);
		}

		// register event
		Object[] previousData = batchedEventDataMap.put(event, invocationContext.getData());
		if (previousData != null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Dropping (outdated) event data " + previousData + ", since newer stuff came in: " + invocationContext.getData());
			}
			return;
		}
		invocationContext.setEventListenerInfo(new IgnorableEventPlaceholder(event), currentEventListener);
		orderedEventList.add(invocationContext);
	}

	private void processBatchedEvents() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Processing " + orderedEventList.size() + " events in one batch...");
		}

		for (InvocationContext ic : orderedEventList) {
			if (ic.getEvent() instanceof IgnorableEventPlaceholder) {
				IgnorableEventPlaceholder placeHolder = (IgnorableEventPlaceholder) ic.getEvent();
				Object realEvent = placeHolder.event;
				Object[] eventData = batchedEventDataMap.get(realEvent);
				Set<EventListener> eventListeners = batchedEventListenerMap.get(placeHolder.event);
				for (EventListener el : eventListeners) {
					ic.setEventListenerInfo(realEvent, el, eventData);
					dispatchEvent(ic);
				}
			}
		}
	}

	private void dispatchEvent(InvocationContext event) {
		event.getEventListener().handle(event.getData());
	}

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) throws Exception {
		if (!endOfBatch) {
			// i.e. events were pumped in faster, than we could process them. In that case,
			// we try to queue them up in memory and hope that we find a few events that we can ignore
			inBatch = true;
			storeForLaterProcessing(event);
		} else {
			if (inBatch) {
				storeForLaterProcessing(event);
				processBatchedEvents();

				// finally, we clear our temp structures
				batchedEventDataMap.clear();
				orderedEventList.clear();
				inBatch = false;

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Event queue cleared");
				}
			} else {
				dispatchEvent(event);
			}
		}
	}

	private class IgnorableEventPlaceholder {
		public final Object event;

		public IgnorableEventPlaceholder(Object event) {
			this.event = event;
		}

		@Override
		public String toString() {
			return "PlaceHolder for " + event;
		}

	}

}
