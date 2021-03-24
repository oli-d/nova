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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

public class RequestFilter implements WebFilter {
    @Autowired
    RequestMappingHandlerMapping requestMappingHandlerMapping;
    @Autowired
    RequestMappingInfoHandlerMapping requestMappingInfoHandlerMapping;

    AtomicInteger ai = new AtomicInteger();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return requestMappingHandlerMapping
                .getHandler(exchange)
                .map(handler -> {
                    HandlerMethod handlerMethod = (HandlerMethod)handler;

                    return ai.incrementAndGet();
                })
                .flatMap(result -> {
                    if (result % 2 == 1) {
                        return chain.filter(exchange);
                    } else {
                        ServerHttpResponse response = exchange.getResponse();
                        response.setStatusCode(HttpStatus.NOT_FOUND);
                        return response.setComplete();
                    }
                });

    }
}
