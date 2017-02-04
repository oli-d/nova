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

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class CurrentThreadEventEmitter extends EventEmitter {
	private final Logger logger = LoggerFactory.getLogger(CurrentThreadEventEmitter.class);

	public CurrentThreadEventEmitter(
			String identifier,
			boolean warnOnUnhandledEvent,
			Metrics metrics) {
		super(identifier, metrics, warnOnUnhandledEvent);
	}

	@Override
	void dispatchEventAndData(Emitter<Object[]>[] emitters, Object event, Object... data) {
		Arrays.stream(emitters).forEach(emitter -> {
			try {
				emitter.onNext(data);
				metricsCollector.eventDispatched(event);
			} catch (Exception e) {
				logger.error("Uncaught exception while invoking onNext() on " + emitter, e);
				logger.error("\tparamters: " + Arrays.toString(data));
			}
		});
	}

}
