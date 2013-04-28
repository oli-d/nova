package com.dotc.nova;

import com.dotc.nova.events.EventListener;

class InvocationContext {
	private Object event;
	private EventListener eventListener;
	private Object[] data;

	private void reset() {
		this.event = null;
		this.eventListener = null;
		this.data = null;
	}

	public Object getEvent() {
		return event;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public Object[] getData() {
		return data;
	}

	public void setEventListenerInfo(Object event, EventListener listener, Object... data) {
		reset();
		this.event = event;
		this.eventListener = listener;
		this.data = data;
	}

}
