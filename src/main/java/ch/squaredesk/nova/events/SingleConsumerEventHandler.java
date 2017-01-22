/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

class SingleConsumerEventHandler implements EventHandler<InvocationContext> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SingleConsumerEventHandler.class);

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) {
		try {
			Object[] data = event.getData();
			for (EventListener listener : event.getEventListeners()) {
				try {
					listener.handle(data);
				} catch (Exception e) {
					LOGGER.error("Caught exception, trying to invoke listener for event " + event, e);
				}
			}
		} finally {
			event.reset();
		}
	}

}
