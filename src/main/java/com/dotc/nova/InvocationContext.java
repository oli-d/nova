package com.dotc.nova;

import com.dotc.nova.events.EventListener;

class InvocationContext {
	private Object event;
	private EventListener eventListener;
	private Runnable callbackToInvoke;

	private void reset() {
		this.event = null;
		this.eventListener = null;
		this.callbackToInvoke = null;
	}

	public boolean isEventListenerContext() {
		return eventListener != null && event != null;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public Object getEvent() {
		return event;
	}

	public <EventType> void setEventListenerInfo(EventType event, EventListener<EventType> listener) {
		reset();
		this.event = event;
		this.eventListener = listener;
	}

	public boolean isCallbackContext() {
		return callbackToInvoke != null;
	}

	public Runnable getCallbackToInvoke() {
		return callbackToInvoke;
	}

	public void setCallbackToInvoke(Runnable callbackToInvoke) {
		reset();
		this.callbackToInvoke = callbackToInvoke;
	}

}
