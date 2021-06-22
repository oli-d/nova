/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.tuples;

public record Tuple3<T, U, V> (T item1, U item2, V item3) {

    public <W> Tuple4<T, U, V, W> add(W w) {
        return new Tuple4<>(item1, item2, item3, w);
    }
}
