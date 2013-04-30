package com.dotc.nova.events;


public interface EventEmitter {

	public void on(Object event, EventListener callback);

	public void once(Object event, EventListener callback);

	public void addListener(Object event, EventListener callback);

	public void removeListener(Object event, EventListener handler);

	public void removeAllListeners(Object event);

	public void emit(Object event, Object... data);
}
