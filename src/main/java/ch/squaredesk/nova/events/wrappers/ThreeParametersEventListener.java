/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events.wrappers;

import ch.squaredesk.nova.events.EventListener;

public interface ThreeParametersEventListener<ParamOneType, ParamTwoType, ParamThreeType> extends EventListener {

	void doHandle(ParamOneType param1, ParamTwoType param2, ParamThreeType param3);

	@SuppressWarnings("unchecked")
	@Override
	default void handle(Object... data) {
		ParamOneType param1 = null;
		ParamTwoType param2 = null;
		ParamThreeType param3 = null;

		if (data != null) {
			if (data.length > 0) {
				param1 = (ParamOneType) data[0];
			}
			if (data.length > 1) {
				param2 = (ParamTwoType) data[1];
			}
			if (data.length > 2) {
				param3 = (ParamThreeType) data[2];
			}
		}

		doHandle(param1, param2, param3);
	}
}