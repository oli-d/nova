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

package ch.squaredesk.nova.comm.http;

import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

public class FrozenHttpAdapter<T> {
    private final HttpAdapter delegate;
    private final Class<T> freezeType;


    FrozenHttpAdapter(HttpAdapter delegate, Class<T> freezeType) {
        this.delegate = delegate;
        this.freezeType = freezeType;
    }

    public void shutdown() {
        delegate.shutdown();
    }

    ///////// The client side
    /////////
    /////////
    /////////////////// GET convenience methods
    public Single<RpcReply<T>> sendGetRequest(String destination) {
        return delegate.sendGetRequest(destination, freezeType);
    }

    public Single<RpcReply<T>> sendGetRequest(
                String destination,
                long timeout, TimeUnit timeUnit) {
        return delegate.sendGetRequest(destination, freezeType, timeout, timeUnit);
    }

    /////////////////// POST convenience methods
    public Single<RpcReply<T>> sendPostRequest(
                String destination,
                T request) {
        return delegate.sendPostRequest(destination, request, freezeType);
    }

    public Single<RpcReply<T>> sendPostRequest(
                String destination,
                T request,
                long timeout, TimeUnit timeUnit) {
        return delegate.sendPostRequest(destination, request, freezeType, timeout, timeUnit );
    }

    /////////////////// PUT convenience methods
    public Single<RpcReply<T>> sendPutRequest(
                String destination,
                T request) {
        return delegate.sendPutRequest(destination, request, freezeType);
    }

    public Single<RpcReply<T>> sendPutRequest(
                String destination,
                T request,
                long timeout, TimeUnit timeUnit) {
        return delegate.sendPutRequest(destination, request, freezeType, timeout, timeUnit );
    }

    /////////////////// DELETE convenience methods
    public Single<RpcReply<T>> sendDeleteRequest(
                String destination,
                T request) {
        return delegate.sendDeleteRequest(destination, request, freezeType);
    }

    public Single<RpcReply<T>> sendDeleteRequest(
                String destination,
                T request,
                long timeout, TimeUnit timeUnit) {
        return delegate.sendDeleteRequest(destination, request, freezeType, timeout, timeUnit );
    }

    /////////////////// convenience methods
    public Single<RpcReply<T>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod) {
        return delegate.sendRequest(destination, request, requestMethod, freezeType);
    }


    public Single<RpcReply<T>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                long timeout,
                TimeUnit timeUnit) {
        return delegate.sendRequest(destination, request, requestMethod, freezeType, timeout, timeUnit);
    }

    /////////////////// and the big one, providing all possibilities
}
