/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events.wrappers;

import ch.squaredesk.nova.events.EventListener;

public interface EightParametersEventListener<ParamOneType, ParamTwoType, ParamThreeType, ParamFourType, ParamFiveType, ParamSixType, ParamSevenType, ParamEightType>
extends EventListener {

	void doHandle(ParamOneType param1, ParamTwoType param2, ParamThreeType param3, ParamFourType param4,
			ParamFiveType param5, ParamSixType param6, ParamSevenType param7, ParamEightType param8);

	@SuppressWarnings("unchecked")
	@Override
	default void handle(Object... data) {
		ParamOneType param1 = null;
		ParamTwoType param2 = null;
		ParamThreeType param3 = null;
		ParamFourType param4 = null;
		ParamFiveType param5 = null;
		ParamSixType param6 = null;
		ParamSevenType param7 = null;
		ParamEightType param8 = null;

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
			if (data.length > 4) {
				param5 = (ParamFiveType) data[4];
			}
			if (data.length > 5) {
				param6 = (ParamSixType) data[5];
			}
			if (data.length > 6) {
				param7 = (ParamSevenType) data[6];
			}
			if (data.length > 7) {
				param8 = (ParamEightType) data[7];
			}
		}

		doHandle(param1, param2, param3, param4, param5, param6, param7, param8);
	}

}
