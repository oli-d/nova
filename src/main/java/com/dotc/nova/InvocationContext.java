package com.dotc.nova;

import com.dotc.nova.events.EventHandler;

class InvocationContext {
	private Object event;
	private EventHandler eventHandler;
	private Object[] data;

	private void reset() {
		this.event = null;
		this.eventHandler = null;
		this.data = null;
	}

	public Object getEvent() {
		return event;
	}

	public EventHandler getEventHandler() {
		return eventHandler;
	}

	public Object[] getData() {
		return data;
	}

	public void setEventListenerInfo(Object event, EventHandler handler, Object... data) {
		reset();
		this.event = event;
		this.eventHandler = handler;
		this.data = data;
	}

}
