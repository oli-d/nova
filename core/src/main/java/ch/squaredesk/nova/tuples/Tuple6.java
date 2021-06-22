/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.tuples;

public record Tuple6<T, U, V, W, X, Y>(T item1, U item2, V item3, W item4, X item5, Y item6) {

    public <Z> Tuple7<T, U, V, W, X, Y, Z> add(Z z) {
        return new Tuple7<>(item1, item2, item3, item4, item5, item6, z);
    }

}
