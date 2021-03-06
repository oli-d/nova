/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.tuples;

public record Tuple4<T, U, V, W>(T item1, U item2, V item3, W item4) {

    public <X> Tuple5<T, U, V, W, X> add(X x) {
        return new Tuple5<>(item1, item2, item3, item4, x);
    }
}
