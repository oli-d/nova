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

import ch.squaredesk.nova.events.metrics.EventMetricsCollector;
import io.reactivex.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CurrentThreadEventEmitter extends EventEmitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrentThreadEventEmitter.class);

	public CurrentThreadEventEmitter(EventMetricsCollector eventMetricsCollector, boolean warnOnUnhandledEvents) {
		super(eventMetricsCollector, warnOnUnhandledEvents);
	}

	@Override
	void dispatchEventAndData(List<Emitter<Object[]>> emitterList, Object event, Object... data) {
		Object[] dataToPass = data.length == 0 ? null : data;
		emitterList.forEach(emitter -> {
			try {
				emitter.onNext(dataToPass);
				metricsCollector.eventDispatched(event);
			} catch (Exception e) {
				LOGGER.error("Uncaught exception while invoking onNext() on " + emitter, e);
				LOGGER.error("\tparamters: " + Arrays.toString(data));
			}
		});
	}

}
