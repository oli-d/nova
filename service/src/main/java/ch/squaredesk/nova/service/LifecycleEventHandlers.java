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

package ch.squaredesk.nova.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class LifecycleEventHandlers {
    private final Logger logger = LoggerFactory.getLogger(LifecycleEventHandlers.class);

    private final CopyOnWriteArrayList<Runnable> initHandlers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> startupHandlers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> shutdownHandlers = new CopyOnWriteArrayList<>();

    public void registerInitHandler (Runnable initHandler) {
        this.initHandlers.add(Objects.requireNonNull(initHandler));
    }

    public void registerStartupHandler (Runnable startupHandler) {
        this.startupHandlers.add(Objects.requireNonNull(startupHandler));
    }

    public void registerShutdownHandler (Runnable shutdownHandler) {
        this.shutdownHandlers.add(Objects.requireNonNull(shutdownHandler));
    }

    public void invokeInitHandlers() {
        invokeHandlers(initHandlers,
                t -> {
                    throw new RuntimeException("Error invoking init handler", t);
                } );
    }

    public void invokeStartupHandlers() {
        invokeHandlers(startupHandlers,
                t -> {
                    throw new RuntimeException("Error invoking startup handler", t);
                });
    }

    public void invokeShutdownHandlers() {
        invokeHandlers(shutdownHandlers,
                t -> logger.warn("Error invoking shutdown handler",t));
    }

    private void invokeHandlers(List<Runnable> handlers, Consumer<Throwable> exceptionHandler) {
        for (Runnable handler: handlers) {
            try {
                handler.run();
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        }
    }
}
