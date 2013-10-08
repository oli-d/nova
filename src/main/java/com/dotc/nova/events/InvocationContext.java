package com.dotc.nova.events;

class InvocationContext {
	private Object event;
	private EventListener eventListener;
	private Object[] data;

	public InvocationContext(Object event, EventListener eventListener, Object... data) {
		this.event = event;
		this.eventListener = eventListener;
		this.data = data;
	}

	public InvocationContext() {
	}

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
