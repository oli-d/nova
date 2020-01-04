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

import ch.squaredesk.nova.tuples.Pair;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                () -> sut.dumpContinuously(-1, TimeUnit.SECONDS));
        assertThat(ex.getMessage(),containsString("interval must be greater than 0"));
        ex = assertThrows(IllegalArgumentException.class,
                () -> sut.dumpContinuously(0, TimeUnit.SECONDS));
        assertThat(ex.getMessage(),containsString("interval must be greater than 0"));
    }

    @Test
    void continuousDumpNeedsNonNullTimeUnit() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> sut.dumpContinuously(5, null));
        assertThat(ex.getMessage(),containsString("timeUnit must not be null"));
    }

    @Test
    void defaultDumpDoesntContainAdditionalInfo() {
        MetricsDump metricsDump = sut.dump();
        MatcherAssert.assertThat(metricsDump.additionalInfo.size(), Matchers.is(0));
    }

    @Test
    void additionalInfoIsIncludedInEveryDump() {
        List<MetricsDump> metricsDumps = sut
                .dumpContinuously(1L, TimeUnit.MILLISECONDS,
                        Arrays.asList(new Pair<>("key", "value"), new Pair<>("oli", "d")))
                .take(3)
                .toList()
                .blockingGet();

        metricsDumps.forEach(metricsDump -> {
            MatcherAssert.assertThat(metricsDump.additionalInfo.size(), Matchers.is(2));
            MatcherAssert.assertThat(metricsDump.additionalInfo.get(0)._1, Matchers.is("key"));
            MatcherAssert.assertThat(metricsDump.additionalInfo.get(0)._2, Matchers.is("value"));
            MatcherAssert.assertThat(metricsDump.additionalInfo.get(1)._1, Matchers.is("oli"));
            MatcherAssert.assertThat(metricsDump.additionalInfo.get(1)._2, Matchers.is("d"));
        });
    }
}
