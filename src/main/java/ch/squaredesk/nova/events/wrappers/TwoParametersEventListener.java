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


public interface TwoParametersEventListener<ParamOneType, ParamTwoType>  {
	@SuppressWarnings("unchecked")
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
