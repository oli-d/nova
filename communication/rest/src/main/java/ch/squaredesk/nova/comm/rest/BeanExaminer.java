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

import javax.ws.rs.Path;
import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

class BeanExaminer {
    private static final Predicate<Annotation> interestingAnnotation = anno -> anno instanceof Path;

    private BeanExaminer() {
    }

    static boolean isRestHandler(Object bean) {
        requireNonNull(bean, "bean to examine must not be null");

        return stream(bean.getClass().getAnnotations())
                        .anyMatch(interestingAnnotation);
    }
}
