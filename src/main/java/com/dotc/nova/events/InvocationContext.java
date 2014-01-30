package com.dotc.nova.events;

import java.util.Arrays;
import java.util.Map;

class InvocationContext {
	private Object event;
	private EventListener[] eventListeners;
	private Object[] data;
	private Object duplicateDetectionId;
	private Map<Object, Object[]> currentDataLookupMap;

	public InvocationContext(Object event, EventListener[] eventListeners, Object... data) {
		this.event = event;
		this.eventListeners = eventListeners;
		this.data = data;
	}

	public InvocationContext() {
	}

	void reset() {
		this.event = null;
		this.eventListeners = null;
		this.data = null;
		this.duplicateDetectionId = null;
		this.currentDataLookupMap = null;
	}

	public Object getEvent() {
		return event;
	}

	public EventListener[] getEventListeners() {
		return eventListeners;
	}

	public Object[] getData() {
		if (duplicateDetectionId != null) {
			return currentDataLookupMap.remove(duplicateDetectionId);
		} else {
			return data;
		}
	}

	public void setEventListenerInfo(Object event, EventListener[] listeners, Object... data) {
		reset();
		this.event = event;
		this.eventListeners = listeners;
		this.data = data;
	}

	public void setEventListenerInfo(Object event, EventListener[] listeners, Object duplicateDetectionId,
			Map<Object, Object[]> currentDataLookupMap) {
		reset();
		this.event = event;
		this.eventListeners = listeners;
		this.duplicateDetectionId = duplicateDetectionId;
		this.currentDataLookupMap = currentDataLookupMap;
	}

	@Override
	public String toString() {
		return "InvocationContext [event=" + event + ", eventListeners=" + Arrays.toString(eventListeners)
				+ (duplicateDetectionId == null ? ", data=" + Arrays.toString(data) : ", duplicateDetectionId=" + duplicateDetectionId)
				+ "]";
	}

}
