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

import javax.ws.rs.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

class BeanExaminer {
    private static final Set<Class> methodAnnotations = new HashSet<>(
            Arrays.asList(PUT.class, GET.class, POST.class, DELETE.class)
    );

    boolean providesRestEndpoint (Object bean) {
        requireNonNull(bean, "bean to examine must not be null");

        return stream(bean.getClass().getDeclaredAnnotations()).anyMatch(anno -> anno instanceof Path) &&
                stream(bean.getClass().getDeclaredMethods()).anyMatch(method ->
                    stream(method.getDeclaredAnnotations()).anyMatch(
                            anno -> anno instanceof GET ||
                                    anno instanceof POST ||
                                    anno instanceof PUT ||
                                    anno instanceof DELETE
                    ) && stream(method.getDeclaredAnnotations()).anyMatch(anno -> anno instanceof Path)
                );
    }

}
