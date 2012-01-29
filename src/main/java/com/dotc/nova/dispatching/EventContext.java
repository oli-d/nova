package com.dotc.nova.dispatching;

import com.dotc.nova.events.EventListener;

public class EventContext<EventType> {
	private EventType event;
	private EventListener<EventType> listener;

	public EventListener<EventType> getListener() {
		return listener;
	}

	public void setListener(EventListener<EventType> listener) {
		this.listener = listener;
	}

	public EventType getEvent() {
		return event;
	}

	public void setEvent(EventType event) {
		this.event = event;
	}

}
