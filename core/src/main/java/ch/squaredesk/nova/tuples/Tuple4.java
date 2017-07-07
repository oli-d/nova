/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.tuples;

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

        if (_1 != null ? !_1.equals(tuple4._1) : tuple4._1 != null) return false;
        if (_2 != null ? !_2.equals(tuple4._2) : tuple4._2 != null) return false;
        if (_3 != null ? !_3.equals(tuple4._3) : tuple4._3 != null) return false;
        return _4 != null ? _4.equals(tuple4._4) : tuple4._4 == null;
    }

    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + (_2 != null ? _2.hashCode() : 0);
        result = 31 * result + (_3 != null ? _3.hashCode() : 0);
        result = 31 * result + (_4 != null ? _4.hashCode() : 0);
        return result;
    }
}
