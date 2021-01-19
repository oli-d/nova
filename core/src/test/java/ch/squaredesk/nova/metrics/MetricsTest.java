/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricsTest {
    private Metrics sut;

    @BeforeEach
    void setUp() {
        sut = new Metrics();
    }

    @Test
    void continuousDumpNeedsIntervalLargerThanZero() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sut.dumpContinuously(Duration.ofSeconds(-1)));
        assertThat(ex.getMessage(),containsString("interval must be positive"));
        ex = assertThrows(IllegalArgumentException.class,
                () -> sut.dumpContinuously(Duration.ZERO));
        assertThat(ex.getMessage(),containsString("interval must be positive"));
    }

    @Test
    void defaultDumpDoesntContainAdditionalInfo() {
        MetricsDump metricsDump = sut.dump();
        MatcherAssert.assertThat(metricsDump.additionalInfo.size(), Matchers.is(0));
    }

    @Test
    void additionalInfoIsIncludedInEveryDump() {
        sut.addAdditionalInfoForDumps("key", "value");
        sut.addAdditionalInfoForDumps("oli", "d");

        List<MetricsDump> metricsDumps = sut
                .dumpContinuously(Duration.ofMillis(1))
                .take(3)
                .toList()
                .blockingGet();

        metricsDumps.forEach(metricsDump -> {
            MatcherAssert.assertThat(metricsDump.additionalInfo.size(), Matchers.is(2));
            MatcherAssert.assertThat(metricsDump.additionalInfo.get(0).item1(), Matchers.is("key"));
            MatcherAssert.assertThat(metricsDump.additionalInfo.get(0).item2(), Matchers.is("value"));
            MatcherAssert.assertThat(metricsDump.additionalInfo.get(1).item1(), Matchers.is("oli"));
            MatcherAssert.assertThat(metricsDump.additionalInfo.get(1).item2(), Matchers.is("d"));
        });
    }
}
