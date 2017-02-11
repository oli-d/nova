package ch.squaredesk.nova.events;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.apache.log4j.BasicConfigurator;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.rmi.NotBoundException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class ThroughputTestDriver {
    private DecimalFormat numberFormatter = new DecimalFormat("#,##0.##");
    static int numTestRuns = 3;
    int numEvents = 10_000_000;
    int numDispatcherThreads = 100;
    int numSubscribersPerTopic = 5;
    int numTopics = 100;

    public static void main (String[] args) throws Exception {
        BasicConfigurator.configure();
        Nova nova = Nova.builder()
                .setIdentifier("perfTest")
                .setEventLoopConfig(EventLoopConfig.builder().setDispatchInEmitterThread(false).build())
                .build();

        ThroughputTestDriver ttd = new ThroughputTestDriver();

        for (int i = 0; i < numTestRuns; i++) {
            System.out.println("=====================" + (i+1) + "/" + numTestRuns + "=================================");
            ttd.go(nova.eventLoop);
        }

        nova.metrics.dumpOnceToLog();
    }

    public void go (EventLoop eventLoop) throws Exception {
        int numEventsPerThread = numEvents / numDispatcherThreads;
        int numEventsTotal = numEvents * numSubscribersPerTopic;
        CountDownLatch cdl = new CountDownLatch(numEventsTotal);

        for (int i = 0; i < numSubscribersPerTopic; i++) {
            for (int topicId = 0; topicId < numTopics; topicId++) {
                eventLoop.on(topicId).subscribe(data -> {
//                   System.out.println(Thread.currentThread().getName() + "/" + data[0] );
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
                    eventLoop.emit(i%numTopics, i);
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
        // System.out.println("Finished...");
    }



}
