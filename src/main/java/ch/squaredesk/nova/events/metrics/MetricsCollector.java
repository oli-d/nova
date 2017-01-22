/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.metrics.Metrics.SettableGauge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MetricsCollector {
    private final Metrics metrics;
    private final String idPrefix;

    private final Set<String> identifiersToBeTracked;
    
    public MetricsCollector(Metrics metrics, String identifierPrefix) {
        this.idPrefix = identifierPrefix == null || identifierPrefix.isEmpty() ? "" : identifierPrefix;
        this.metrics = metrics;
        this.identifiersToBeTracked = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    public void setTrackingEnabled(boolean enabled, String... identifiers) {
        if (identifiers == null || identifiers.length == 0) {
            return;
        }

        for (String identifier : identifiers) {
            if (enabled) {
                identifiersToBeTracked.add(identifier);
            } else {
                identifiersToBeTracked.remove(identifier);
            }
        }
    }

    protected boolean shouldBeTracked(String identifier) {
        return identifier != null && identifiersToBeTracked.contains(identifier);
    }

    private String[] concatPrefixWithIdentifier(String... identifier) {
        if (identifier == null || identifier.length == 0) {
            throw new IllegalArgumentException("identifier must be set");
        } else {
            List<String> identifiers = new ArrayList<>(Arrays.asList(identifier));
            identifiers.add(0, idPrefix);
            return identifiers.toArray(new String[identifiers.size()]);
        }
    }

    protected com.codahale.metrics.Timer getTimer(String... identifier) {
        String[] prefixedIdentifier = concatPrefixWithIdentifier(identifier);
        return metrics.getTimer(prefixedIdentifier);
    }

    protected Meter getMeter(String... identifier) {
        String[] prefixedIdentifier = concatPrefixWithIdentifier(identifier);
        return metrics.getMeter(prefixedIdentifier);
    }

    protected Counter getCounter(String... identifier) {
        String[] prefixedIdentifier = concatPrefixWithIdentifier(identifier);
        return metrics.getCounter(prefixedIdentifier);
    }

    protected SettableGauge getGauge(String... identifier) {
        String[] prefixedIdentifier = concatPrefixWithIdentifier(identifier);
        return metrics.getGauge(prefixedIdentifier);
    }

    protected boolean remove(String... identifier) {
        String[] prefixedIdentifier = concatPrefixWithIdentifier(identifier);
        return metrics.remove(prefixedIdentifier);
    }
}
