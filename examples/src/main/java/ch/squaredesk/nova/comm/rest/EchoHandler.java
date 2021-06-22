/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@RestController
public class EchoHandler {
    public static record FormData(String xxx) {
    }

    @PostMapping(value = "/form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<String>> echoFormParameterValue(Mono<FormData> formData) {
        return formData
            .map(fd -> new ResponseEntity<>(
                Optional.ofNullable(fd).map(FormData::xxx).orElse("unknown"),
                HttpStatus.OK
        ));
    }

    @GetMapping("/echo")
    public String echoParameterValue(@RequestParam("p1") String textFromCallerToEcho) {
        return textFromCallerToEcho;
    }

    @GetMapping("/echo/{text}")
    public String echoPathValue(@PathVariable("text") String textFromCallerToEcho) {
        System.out.println("Hallo");
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
