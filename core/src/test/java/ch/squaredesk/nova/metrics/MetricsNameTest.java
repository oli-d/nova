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

package ch.squaredesk.nova.metrics;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MetricsNameTest {
    @Test
    void namesProperlyBuilt() {
        assertThat(MetricsName.buildName((String[]) null), is(""));
        assertThat(MetricsName.buildName((String) null), is(""));
        assertThat(MetricsName.buildName(null, null), is(""));
        assertThat(MetricsName.buildName(null, ""), is(""));
        assertThat(MetricsName.buildName(null, "hallo"), is("hallo"));
        assertThat(MetricsName.buildName("hallo", null), is("hallo"));
        assertThat(MetricsName.buildName("hallo", "welt"), is("hallo.welt"));
        assertThat(MetricsName.buildName("hallo", "welt", "oli"), is("hallo.welt.oli"));
    }
}