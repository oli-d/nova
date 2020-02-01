/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.tuples;

import java.util.Objects;

public class Tuple3<T, U, V> {
    public final T _1;
    public final U _2;
    public final V _3;

    public Tuple3(T t, U u, V v) {
        _1 = t;
        _2 = u;
        _3 = v;
    }

    public <W> Tuple4<T, U, V, W> add(W w) {
        return new Tuple4<>(_1, _2, _3, w);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
        return Objects.equals(_1, tuple3._1) &&
                Objects.equals(_2, tuple3._2) &&
                Objects.equals(_3, tuple3._3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3);
    }
}
