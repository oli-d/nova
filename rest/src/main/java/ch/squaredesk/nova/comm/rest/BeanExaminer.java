/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.comm.rest.annotation.OnRestRequest;
import ch.squaredesk.nova.comm.rest.annotation.RestEndpointDescription;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

class BeanExaminer {
    private final Predicate<Annotation> interestingAnnotation = anno -> anno instanceof OnRestRequest;

    RestEndpointDescription[] restEndpointsIn (Object bean) {
        requireNonNull(bean, "bean to examine must not be null");

        return stream(bean.getClass().getDeclaredMethods())
                .filter(method -> stream(method.getDeclaredAnnotations()).anyMatch(interestingAnnotation))
                .map(method -> {
                    OnRestRequest annotation = stream(method.getDeclaredAnnotations())
                            .filter(interestingAnnotation)
                            .findFirst()
                            .map(anno -> (OnRestRequest)anno)
                            .get();
                    return new RestEndpointDescription(annotation.value(),
                            annotation.produces(),
                            annotation.consumes(),
                            annotation.requestMethod(),
                            method);
                })
                .toArray(RestEndpointDescription[]::new);
    }
}
