/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

import ch.squaredesk.nova.Nova;

import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class ThroughputTestDriver {
    private static DecimalFormat numberFormatter = new DecimalFormat("#,##0.##");
    static int numTestRuns = 3;
    static int numEvents = 10_000_000;
    static int numDispatcherThreads = 100;
    static int numSubscribersPerTopic = 5;
    static int numTopics = 100;
    static int numEventsPerThread = numEvents / numDispatcherThreads;
    static int numEventsTotal = numEvents * numSubscribersPerTopic;

    public static void main (String[] args) throws Exception {
        Nova nova = Nova.builder()
                .setIdentifier("perfTest")
                .build();

        ThroughputTestDriver ttd = new ThroughputTestDriver();

        System.out.println("numEventsPerThread = " + numberFormatter.format(numEventsPerThread));
        System.out.println("numDispatcherThreads = " + numberFormatter.format(numDispatcherThreads));

        for (int i = 0; i < numTestRuns; i++) {
            System.out.println("=====================" + (i+1) + "/" + numTestRuns + "=================================");
            ttd.go(nova.eventBus);
        }

        nova.metrics.dumpToLog();
    }

    public void go (EventBus eventBus) throws Exception {
        CountDownLatch cdl = new CountDownLatch(numEventsTotal);

        for (int i = 0; i < numSubscribersPerTopic; i++) {
            for (int topicId = 0; topicId < numTopics; topicId++) {
                eventBus.on(topicId).subscribe(data -> {
                   //System.out.println(Thread.currentThread().getName() + "/" + data[0] );
                    cdl.countDown();
                });
            }
        }

        long now = System.currentTimeMillis();
        Thread[] threads = new Thread[numDispatcherThreads];
        for (int x=0; x<numDispatcherThreads; x++) {
            int idx = x;
            threads[x] = new Thread(() -> {
                for (int i=0; i<numEventsPerThread; i++) {
                    eventBus.emit(i%numTopics, i);
                }
                // System.err.println("Thread " + idx + " done emitting!");
            });
        }
        for (int x=0; x<numDispatcherThreads; x++) {
            threads[x].start();
        }

        cdl.await(14, TimeUnit.SECONDS);
        long delta = System.currentTimeMillis() - now;

        long numEventsNotBeingDispatchedInTime = cdl.getCount();

        System.out.println(String.format("It took %s seconds to dispatch %s events with %s emitters on %s topics, subscribersPerTopic=%s",
                numberFormatter.format((double)delta / 1000.0),
                numberFormatter.format(numEventsTotal - numEventsNotBeingDispatchedInTime),
                numDispatcherThreads,
                numTopics,
                numSubscribersPerTopic
                ));
        System.out.println(String.format("\tThis equals to %s events per second",
                numberFormatter.format((double)(numEventsTotal - numEventsNotBeingDispatchedInTime) / ((double)delta / 1000.0))
                ));
        if (numEventsNotBeingDispatchedInTime>0) {
            System.out.println(String.format("\t%s of the originally planned %s events were not dispatched, this equals to %s percent!!!!",
                    numberFormatter.format(numEventsNotBeingDispatchedInTime),
                    numberFormatter.format(numEventsTotal),
                    (double)numEventsNotBeingDispatchedInTime/(double)numEventsTotal*100));
        }
        for (int x=0; x<numDispatcherThreads; x++) {
            threads[x].join();
        }
        System.out.println("Finished...");
    }



}
