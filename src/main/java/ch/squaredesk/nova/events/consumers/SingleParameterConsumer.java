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
public interface SingleParameterConsumer<P1>  extends Consumer<Object[]> {

    void consume(P1 param1);

    default void accept(Object... data) {
        P1 p1 = null;
        if (data != null && data.length > 0) {
            switch (data.length) {
                default:
                case 1: p1 = (P1)data[0];
            }
        }

        try {
            consume(p1);
        } catch (Throwable t) {
            LoggerFactory.getLogger("ch.squaredesk.nova.event.consumers")
                    .error("Error, trying to consume event with parameters " +
                            Arrays.toString(data));
        }
    }
}
