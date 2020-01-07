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

package ch.squaredesk.nova;

import ch.squaredesk.nova.events.EventDispatchMode;
import io.reactivex.BackpressureStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nova.events")
public class EventDispatchConfigurationProperties {
    /** Specifies if the system should log a warning when an event is emitted, for which no handler was registered */
    private Boolean warnOnUnhandledEvent = false;
    /** The default backpressure strategy to use for the event Flowables */
    private BackpressureStrategy backpressureStrategy = BackpressureStrategy.BUFFER;
    /** The default eventDispatchMode */
    private EventDispatchMode eventDispatchMode = EventDispatchMode.BLOCKING;
    /**
     * The parallelism. Only applicable if eventDispatchMode is NON_BLOCKING_FIXED_THREAD_POOL or
     * NON_BLOCKING_WORK_STEALING_POOL. If using a fixed thread pool, parallelism defines the number of threads.
     * If using a work stealing pool, parallelism may be 0 to signal the default setting (use all available CPU if needed)Ø
     */
    private int parallelism = 0;

    public Boolean getWarnOnUnhandledEvent() {
        return warnOnUnhandledEvent;
    }

    public void setWarnOnUnhandledEvent(Boolean warnOnUnhandledEvent) {
        this.warnOnUnhandledEvent = warnOnUnhandledEvent;
    }

    public BackpressureStrategy getBackpressureStrategy() {
        return backpressureStrategy;
    }

    public void setBackpressureStrategy(BackpressureStrategy backpressureStrategy) {
        this.backpressureStrategy = backpressureStrategy;
    }

    public EventDispatchMode getEventDispatchMode() {
        return eventDispatchMode;
    }

    public void setEventDispatchMode(EventDispatchMode eventDispatchMode) {
        this.eventDispatchMode = eventDispatchMode;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }
}
