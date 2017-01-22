/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events.wrappers;

import ch.squaredesk.nova.events.EventListener;

public interface FourParametersEventListener<ParamOneType, ParamTwoType, ParamThreeType, ParamFourType> extends
        EventListener {

	public abstract void doHandle(ParamOneType param1, ParamTwoType param2, ParamThreeType param3, ParamFourType param4);

	@Override
	@SuppressWarnings("unchecked")
	public default void handle(Object... data) {
		ParamOneType param1 = null;
		ParamTwoType param2 = null;
		ParamThreeType param3 = null;
		ParamFourType param4 = null;

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
			if (data.length > 3) {
				param4 = (ParamFourType) data[3];
			}
		}

		doHandle(param1, param2, param3, param4);
	}
}
