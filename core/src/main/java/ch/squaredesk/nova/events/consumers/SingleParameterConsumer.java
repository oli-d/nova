/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events.consumers;


import io.reactivex.rxjava3.functions.Consumer;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static ch.squaredesk.nova.events.consumers.ParamHelper.elementAtIndex;

@FunctionalInterface
public interface SingleParameterConsumer<P1>  extends Consumer<Object[]> {

    void consume(P1 param1);

    @SuppressWarnings("unchecked")
    default void accept(Object... data) {
        try {
            consume((P1) elementAtIndex(0, data));
        } catch (Exception e) {
            LoggerFactory
                    .getLogger("ch.squaredesk.nova.event.consumers")
                    .error("Error, trying to consume event with parameters {}", Arrays.toString(data), e);
        }
    }
}
