/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.tuples;

import java.util.Objects;

public class Tuple4<T, U, V, W> {
    public final T _1;
    public final U _2;
    public final V _3;
    public final W _4;

    public Tuple4(T _1, U _2, V _3, W _4) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
        this._4 = _4;
    }

    public <X> Tuple5<T, U, V, W, X> add(X x) {
        return new Tuple5<>(_1, _2, _3, _4, x);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple4<?, ?, ?, ?> tuple4 = (Tuple4<?, ?, ?, ?>) o;
        return Objects.equals(_1, tuple4._1) &&
                Objects.equals(_2, tuple4._2) &&
                Objects.equals(_3, tuple4._3) &&
                Objects.equals(_4, tuple4._4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3, _4);
    }
}
