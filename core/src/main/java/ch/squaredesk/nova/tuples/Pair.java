/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.tuples;

public record Pair<T,U>  (T item1, U item2) {

    public <V> Tuple3<T,U,V> add (V v) {
        return new Tuple3<>(item1, item2, v);
    }

    public static <T, U> Pair<T,U> create (T t, U u) {
        return new Pair<>(t, u);
    }
}
