/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.events.annotation;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class NovaSchedulers {
    private static final ThreadFactory threadFactoryBizLogic = runnable -> {
        Thread t = new Thread(runnable, "NovaEventDispatcher");
        t.setDaemon(true);
        return t;
    };

    public static Scheduler eventDispatchScheduler(int threadPoolSize) {
        if (threadPoolSize == 1) {
            return Schedulers.from(Executors.newSingleThreadExecutor(threadFactoryBizLogic));
        } else {
            return Schedulers.from(Executors.newFixedThreadPool(threadPoolSize));
        }

    }
}
