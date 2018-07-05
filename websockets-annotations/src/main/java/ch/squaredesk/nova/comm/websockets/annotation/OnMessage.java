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

package ch.squaredesk.nova.comm.websockets.annotation;

import io.reactivex.BackpressureStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnMessage {
    String value();
    boolean captureTimings() default true;
    BackpressureStrategy backpressureStrategy() default BackpressureStrategy.BUFFER;
    String messageMarshallerClassName() default "";
    String messageUnmarshallerClassName() default "";
    // TODO boolean dispatchOnBusinessLogicThread() default false;
}
