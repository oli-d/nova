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

        if (_1 != null ? !_1.equals(tuple7._1) : tuple7._1 != null) return false;
        if (_2 != null ? !_2.equals(tuple7._2) : tuple7._2 != null) return false;
        if (_3 != null ? !_3.equals(tuple7._3) : tuple7._3 != null) return false;
        if (_4 != null ? !_4.equals(tuple7._4) : tuple7._4 != null) return false;
        if (_5 != null ? !_5.equals(tuple7._5) : tuple7._5 != null) return false;
        if (_6 != null ? !_6.equals(tuple7._6) : tuple7._6 != null) return false;
        return _7 != null ? _7.equals(tuple7._7) : tuple7._7 == null;
    }

    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + (_2 != null ? _2.hashCode() : 0);
        result = 31 * result + (_3 != null ? _3.hashCode() : 0);
        result = 31 * result + (_4 != null ? _4.hashCode() : 0);
        result = 31 * result + (_5 != null ? _5.hashCode() : 0);
        result = 31 * result + (_6 != null ? _6.hashCode() : 0);
        result = 31 * result + (_7 != null ? _7.hashCode() : 0);
        return result;
    }
}
