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


public interface ThreeParametersEventListener<ParamOneType, ParamTwoType, ParamThreeType>  {

	void doHandle(ParamOneType param1, ParamTwoType param2, ParamThreeType param3);

	@SuppressWarnings("unchecked")
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
