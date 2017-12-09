/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest.annotation;

import ch.squaredesk.nova.comm.http.HttpRequestMethod;
import ch.squaredesk.nova.comm.http.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnRestRequest {
    String value();
    MediaType[] consumes() default { MediaType.TEXT_PLAIN };
    MediaType produces() default MediaType.TEXT_PLAIN;
    HttpRequestMethod requestMethod() default HttpRequestMethod.GET;
    boolean dispatchOnBusinessLogicThread() default false;
}
