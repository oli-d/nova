/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.WorkHandler;

class DefaultWorkHandler implements WorkHandler<InvocationContext> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkHandler.class);

	@Override
	public void onEvent(InvocationContext event) {
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
