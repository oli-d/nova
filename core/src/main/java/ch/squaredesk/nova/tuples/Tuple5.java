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

public class Tuple5<T, U, V, W, X> {
    public final T _1;
    public final U _2;
    public final V _3;
    public final W _4;
    public final X _5;

    public Tuple5(T _1, U _2, V _3, W _4, X _5) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
        this._4 = _4;
        this._5 = _5;
    }

    public <Y> Tuple6<T, U, V, W, X, Y> add(Y y) {
        return new Tuple6<>(_1, _2, _3, _4, _5, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple5<?, ?, ?, ?, ?> tuple5 = (Tuple5<?, ?, ?, ?, ?>) o;

        if (_1 != null ? !_1.equals(tuple5._1) : tuple5._1 != null) return false;
        if (_2 != null ? !_2.equals(tuple5._2) : tuple5._2 != null) return false;
        if (_3 != null ? !_3.equals(tuple5._3) : tuple5._3 != null) return false;
        if (_4 != null ? !_4.equals(tuple5._4) : tuple5._4 != null) return false;
        return _5 != null ? _5.equals(tuple5._5) : tuple5._5 == null;
    }

    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + (_2 != null ? _2.hashCode() : 0);
        result = 31 * result + (_3 != null ? _3.hashCode() : 0);
        result = 31 * result + (_4 != null ? _4.hashCode() : 0);
        result = 31 * result + (_5 != null ? _5.hashCode() : 0);
        return result;
    }
}
