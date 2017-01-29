/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
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
