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
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@FunctionalInterface
public interface ThreeParameterConsumer<
        P1,
        P2,
        P3>  extends Consumer<Object[]> {

    void accept(P1 param1, P2 param2, P3 param3);

    default void accept(Object... data) {
        P1 p1 = null;
        P2 p2 = null;
        P3 p3 = null;
        if (data != null && data.length > 0) {
            switch (data.length) {
                default:
                case 3: p3 = (P3)data[2];
                case 2: p2 = (P2)data[1];
                case 1: p1 = (P1)data[0];
            }
        }

        try {
            accept(p1, p2, p3);
        } catch (Throwable t) {
            LoggerFactory.getLogger("ch.squaredesk.nova.event.consumers")
                    .error("Error, trying to consume event with parameters " +
                            Arrays.toString(data));
        }
    }
}
