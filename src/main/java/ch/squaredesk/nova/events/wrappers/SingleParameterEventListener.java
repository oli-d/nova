/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events.wrappers;

import ch.squaredesk.nova.events.EventListener;

public interface SingleParameterEventListener<ParamType> extends EventListener {
	@SuppressWarnings("unchecked")
	@Override
	default void handle(Object... params) {
		if (params == null || params.length == 0) {
			doHandle(null);
		} else {
			doHandle((ParamType) params[0]);
		}
	}

	void doHandle(ParamType p);
}
