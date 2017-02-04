/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events.consumers;


import io.reactivex.functions.Consumer;

@FunctionalInterface
public interface TwoParameterConsumer<
        P1,
        P2>  extends Consumer<Object[]> {

	void accept(P1 param1, P2 param2);

	default void accept(Object... data) {
        P1 p1 = null;
        P2 p2 = null;
        if (data != null && data.length > 0) {
            switch (data.length) {
                default:
                case 2: p2 = (P2)data[1];
                case 1: p1 = (P1)data[0];
            }
        }

		accept(p1, p2);
	}
}
