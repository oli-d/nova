/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MetricsTest {
    private Metrics sut;

    @BeforeEach
    public void setUp() {
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
    void dumpIncludesServerDetails() throws Exception {
        MetricsDump dump = sut.dump();

        InetAddress inetAddress = InetAddress.getLocalHost();
        assertThat(dump.hostName, is(inetAddress.getHostName()));
        assertThat(dump.hostAddress, is(inetAddress.getHostAddress()));
    }

}
