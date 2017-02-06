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

import com.lmax.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultWorkHandler implements WorkHandler<InvocationContext> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkHandler.class);

	@Override
	public void onEvent(InvocationContext event) {
		try {
			event.getConsumer().accept(event.getData());
		} catch (Exception e) {
			LOGGER.error("Caught exception, trying to invoke emitter for event " + event, e);
		} finally {
			event.reset();
		}
	}

}
