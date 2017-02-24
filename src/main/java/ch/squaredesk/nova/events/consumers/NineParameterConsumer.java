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
public interface NineParameterConsumer<
        P1,
        P2,
        P3,
        P4,
        P5,
        P6,
        P7,
        P8,
        P9>  extends Consumer<Object[]> {

    void accept(P1 param1, P2 param2, P3 param3, P4 param4,
                P5 param5, P6 param6, P7 param7, P8 param8,
                P9 param9);

    default void accept(Object... data) {
        P1 p1 = null;
        P2 p2 = null;
        P3 p3 = null;
        P4 p4 = null;
        P5 p5 = null;
        P6 p6 = null;
        P7 p7 = null;
        P8 p8 = null;
        P9 p9 = null;
        if (data != null && data.length > 0) {
            switch (data.length) {
                default:
                case 9: p9 = (P9)data[8];
                case 8: p8 = (P8)data[7];
                case 7: p7 = (P7)data[6];
                case 6: p6 = (P6)data[5];
                case 5: p5 = (P5)data[4];
                case 4: p4 = (P4)data[3];
                case 3: p3 = (P3)data[2];
                case 2: p2 = (P2)data[1];
                case 1: p1 = (P1)data[0];
            }
        }

        try {
            accept(p1, p2, p3, p4, p5, p6, p7, p8, p9);
        } catch (Throwable t) {
            LoggerFactory.getLogger("ch.squaredesk.nova.event.consumers")
                    .error("Error, trying to consume event with parameters " +
                            Arrays.toString(data));
        }
    }
}
