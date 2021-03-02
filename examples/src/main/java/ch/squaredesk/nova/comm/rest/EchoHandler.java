/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class EchoHandler {
    @GetMapping("/echo")
    public String echoParameterValue(@RequestParam("p1") String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }

    @GetMapping("/echo/{text}")
    public String echoPathValue(@PathVariable("text") String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }

    @GetMapping("/echo/async/{text}")
    public Mono<ResponseEntity<String>> echoPathValueAsync(@PathVariable("text") String textFromCallerToEcho) {
        return Mono.just(ResponseEntity.ok(textFromCallerToEcho)).delayElement(Duration.ofSeconds(5));
    }

    @PostMapping("/echo")
    public String echoPostRequestBody(@RequestBody String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }

}
