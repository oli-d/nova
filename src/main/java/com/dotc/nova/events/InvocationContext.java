package com.dotc.nova.events;

import java.util.Arrays;
import java.util.Map;

class InvocationContext {
	private Object event;
	private EventListener eventListener;
	private Object[] data;
	private Object duplicateDetectionId;
	private Map<Object, Object[]> currentDataLookupMap;

	public InvocationContext(Object event, EventListener eventListener, Object... data) {
		this.event = event;
		this.eventListener = eventListener;
		this.data = data;
	}

	public InvocationContext() {
	}

	void reset() {
		this.event = null;
		this.eventListener = null;
		this.data = null;
		this.duplicateDetectionId = null;
		this.currentDataLookupMap = null;
	}

	public Object getEvent() {
		return event;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public Object[] getData() {
		if (duplicateDetectionId != null) {
			return currentDataLookupMap.remove(duplicateDetectionId);
		} else {
			return data;
		}
	}

	public void setEventListenerInfo(Object event, EventListener listener, Object... data) {
		reset();
		this.event = event;
		this.eventListener = listener;
		this.data = data;
	}

	public void setEventListenerInfo(Object event, EventListener listener, Object duplicateDetectionId,
			Map<Object, Object[]> currentDataLookupMap) {
		reset();
		this.event = event;
		this.eventListener = listener;
		this.duplicateDetectionId = duplicateDetectionId;
		this.currentDataLookupMap = currentDataLookupMap;
	}

	@Override
	public String toString() {
		return "InvocationContext [event=" + event + ", eventListener=" + eventListener
				+ (duplicateDetectionId == null ? ", data=" + Arrays.toString(data) : ", duplicateDetectionId=" + duplicateDetectionId)
				+ "]";
	}

}
