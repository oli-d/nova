/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events.wrappers;

import ch.squaredesk.nova.events.EventListener;

public interface NoParameterEventListener extends EventListener {

	void doHandle();

	@Override
	default void handle(Object... data) {
		doHandle();
	}

}
