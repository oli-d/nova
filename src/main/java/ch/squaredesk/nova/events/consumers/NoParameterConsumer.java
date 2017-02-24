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
public interface NoParameterConsumer extends Consumer<Object[]> {

    void accept();

    default void accept(Object... data) {
        try {
            accept();
        } catch (Throwable t) {
            LoggerFactory.getLogger("ch.squaredesk.nova.event.consumers")
                    .error("Error, trying to consume event with parameters " +
                            Arrays.toString(data));
        }
    }
}
