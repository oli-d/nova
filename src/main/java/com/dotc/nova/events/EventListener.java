package com.dotc.nova.events;

public interface EventListener<EventType> {
	public void handle(EventType event);
}
