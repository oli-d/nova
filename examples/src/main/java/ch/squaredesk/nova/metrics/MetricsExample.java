/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.events.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Random;

/**
 * This example shows a very simple use case for the Nova metrics feature.
 */
@SpringBootApplication
public class MetricsExample implements CommandLineRunner {
    private static Random rand = new Random();

    @Autowired
    Nova nova;

    public static void main(String[] args) {
        SpringApplication.run(MetricsExample.class, args);
    }

    private static void startEventCreation(final EventBus eventBus) {
        Thread t = new Thread(() -> {
            for (;;) {
                long sleepTimeMillis = (long) (rand.nextDouble() * 1000);
                try {
                    Thread.sleep(sleepTimeMillis);
                } catch (InterruptedException e) {
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
                }
                eventBus.emit("Event");
                eventBus.emit("Event2");
                eventBus.emit("Unhandled");
                eventBus.emit("Unhandled2");
            }
        });
        t.setDaemon(true);
        t.start();

    }

    @Override
    public void run(String... args) throws Exception {
        /**
         * ****************************************************************** *
         * ***                                                            *** *
         * *** Specify that metrics should regularly be dumped to logfile *** *
         * ***                                                            *** *
         * ****************************************************************** *
         */
        nova.metrics.dumpContinuouslyToLog(Duration.ofSeconds(2));

        /**
         * *************************************************************************** *
         * ***                                                                     *** *
         * *** 3rd step:                                                           *** *
         * *** register dummy listeners and create events until <ENTER> is pressed *** *
         * ***                                                                     *** *
         * *************************************************************************** *
         */
        nova.eventBus.on("Event").subscribe();
        nova.eventBus.on("Event2").subscribe();
        startEventCreation(nova.eventBus);

        String s = new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

}
