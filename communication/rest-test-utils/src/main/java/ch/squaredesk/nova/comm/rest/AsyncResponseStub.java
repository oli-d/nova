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

package ch.squaredesk.nova.comm.rest;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AsyncResponseStub implements AsyncResponse {
    private boolean suspended;
    private Object entity;
    private boolean cancelled;
    public long timeout;
    private TimeUnit timeUnit;
    private TimeoutHandler timeoutHandler;

    public <T> T castEntity (Class<T> classToCastTo) {
        return (T) entity;
    }

    @Override
    public boolean resume(Object response) {
        entity = response;
        suspended = false;
        return true;
    }

    @Override
    public boolean resume(Throwable response) {
        entity = response;
        suspended = false;
        return true;
    }

    @Override
    public boolean cancel() {
        cancelled = true;
        suspended = false;
        return true;
    }

    @Override
    public boolean cancel(int retryAfter) {
        cancelled = true;
        suspended = false;
        return true;
    }

    @Override
    public boolean cancel(Date retryAfter) {
        cancelled = true;
        suspended = false;
        return true;
    }

    @Override
    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return !suspended;
    }

    @Override
    public boolean setTimeout(long time, TimeUnit unit) {
        this.timeout = time;
        this.timeUnit = unit;
        return true;
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler handler) {
        this.timeoutHandler = handler;
    }

    @Override
    public Collection<Class<?>> register(Class<?> callback) {
        return Collections.emptyList();
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback, Class<?>... callbacks) {
        return null;
    }

    @Override
    public Collection<Class<?>> register(Object callback) {
        return Collections.emptyList();
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Object callback, Object... callbacks) {
        return null;
    }
}
