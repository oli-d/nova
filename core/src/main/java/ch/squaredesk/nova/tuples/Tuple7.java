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

public class Tuple7<T, U, V, W, X, Y, Z> {
    public final T _1;
    public final U _2;
    public final V _3;
    public final W _4;
    public final X _5;
    public final Y _6;
    public final Z _7;

    public Tuple7(T _1, U _2, V _3, W _4, X _5, Y _6, Z _7) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
        this._4 = _4;
        this._5 = _5;
        this._6 = _6;
        this._7 = _7;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple7<?, ?, ?, ?, ?, ?, ?> tuple7 = (Tuple7<?, ?, ?, ?, ?, ?, ?>) o;
        return Objects.equals(_1, tuple7._1) &&
                Objects.equals(_2, tuple7._2) &&
                Objects.equals(_3, tuple7._3) &&
                Objects.equals(_4, tuple7._4) &&
                Objects.equals(_5, tuple7._5) &&
                Objects.equals(_6, tuple7._6) &&
                Objects.equals(_7, tuple7._7);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3, _4, _5, _6, _7);
    }
}
