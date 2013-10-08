package com.dotc.nova.events;

/**
 * By implementing this interface, an event class signals that the represented event might be ignored, if it has not yet been dispatched, and a newer one exists, which returns the same event. In such a
 * situation, all events except the very latest one, will be ignored.
 * 
 * A typical scenario might be a price feed for a particular stock. If the ticks come in faster, than the consumer is able to process them, old (not yet processed) ones will be discarded and only the
 * latest one will be dispatched to the consumer.
 * 
 */
public interface IgnorableEvent {
	public String getKey();
}
