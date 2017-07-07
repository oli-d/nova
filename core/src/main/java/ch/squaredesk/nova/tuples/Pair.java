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

public class Pair<T,U> {
    public final T _1;
    public final U _2;

    public Pair(T t, U u) {
        _1 = t;
        _2 = u;
    }

    public <V> Tuple3<T,U,V> add (V v) {
        return new Tuple3<>(_1, _2, v);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (_1 != null ? !_1.equals(pair._1) : pair._1 != null) return false;
        return _2 != null ? _2.equals(pair._2) : pair._2 == null;
    }

    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + (_2 != null ? _2.hashCode() : 0);
        return result;
    }
}
