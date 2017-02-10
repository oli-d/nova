package ch.squaredesk.nova.events;

import ch.squaredesk.nova.metrics.Metrics;
import org.apache.log4j.BasicConfigurator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class ThroughputTestDriver {

    public static void main (String[] args) throws InterruptedException {
        BasicConfigurator.configure();
        EventLoop eventLoop = new EventLoop("main()", EventLoopConfig.builder().setDispatchInEmitterThread(false).build(), new Metrics());

        int numEvents = 10_000_000;
        int numDispatcherThreads = 10;
        int numSubscribersPerTopic = 1;
        int numTopics = 1;
        int numEventsPerThread = numEvents / numDispatcherThreads;
        CountDownLatch cdl = new CountDownLatch(numEvents * numSubscribersPerTopic);

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
                System.err.println("Thread " + idx + " done emitting!");
            });
        }
        for (int x=0; x<numDispatcherThreads; x++) {
            threads[x].start();
        }

        cdl.await(14, TimeUnit.SECONDS);
        long delta = System.currentTimeMillis() - now;
        System.err.println(cdl.getCount());
        System.out.println("It took " + ((double)delta / 1000.0) + " s to disptach " + (numEvents*numSubscribersPerTopic) + " events");
    }

}
