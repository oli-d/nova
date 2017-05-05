/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UIDCorrelationIdGeneratorTest {

    @Test
    void testIdsAreUniqueOverMultipleInstances() {
        int numGenerators = 5;
        int numIds = 100000;

        UIDCorrelationIdGenerator[] generators = new UIDCorrelationIdGenerator[numGenerators];
        for (int generator = 0; generator < numGenerators; generator++) {
            generators[generator] = new UIDCorrelationIdGenerator();
        }

        HashSet<String> idSet = new HashSet<>();
        for (int id = 0; id < numIds; id++) {
            for (int generator = 0; generator < numGenerators; generator++) {
                idSet.add(generators[generator].get());
            }
        }

        assertEquals(idSet.size(), numGenerators * numIds);
    }

}
