/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events.wrappers;

import ch.squaredesk.nova.events.EventListener;

public interface TwoParametersEventListener<ParamOneType, ParamTwoType> extends EventListener {
	@SuppressWarnings("unchecked")
	@Override
	default void handle(Object... params) {
		if (params == null || params.length == 0) {
			doHandle(null, null);
		} else if (params == null || params.length == 1) {
			doHandle((ParamOneType) params[0], null);
		} else {
			doHandle((ParamOneType) params[0], (ParamTwoType) params[1]);
		}
	}

	void doHandle(ParamOneType p1, ParamTwoType p2);
}
