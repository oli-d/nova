/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
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
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AsyncResponseStub implements AsyncResponse {
    private boolean suspended;
    public Response entityAsResponse;
    public String entityAsString;
    public Throwable entityAsError;
    public boolean cancelled;
    public long timeout;
    public TimeUnit timeUnit;
    public TimeoutHandler timeoutHandler;

    @Override
    public boolean resume(Object response) {
        if (response != null) {
            if (response instanceof Response) entityAsResponse = (Response)response;
            else entityAsString = String.valueOf(response);
        }
        suspended = false;
        return true;
    }

    @Override
    public boolean resume(Throwable response) {
        entityAsError = response;
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
        return null;
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback, Class<?>... callbacks) {
        return null;
    }

    @Override
    public Collection<Class<?>> register(Object callback) {
        return null;
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Object callback, Object... callbacks) {
        return null;
    }
}
