package ch.squaredesk.nova.comm;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BackpressuredStreamPerformance {
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    static {
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setGroupingUsed(true);
    }

    int numMessagesToDispatch = 1_000_000;

    private void go() throws Exception {
        System.out.println("\n\nDispatching " + numberFormat.format(numMessagesToDispatch) + " messages via backpressured stream");
        invokeAndTime(() -> {
            try {
                dispatchUsingBackpressuredStream(numMessagesToDispatch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.out.println("\n\nDispatching " + numberFormat.format(numMessagesToDispatch) + " messages via subject");
        invokeAndTime(() -> {
            try {
                dispatchUsingPublishSubject(numMessagesToDispatch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.out.println("\n\nDispatching " + numberFormat.format(numMessagesToDispatch) + " messages via listener");
        invokeAndTime(() -> {
            try {
                dispatchUsingListener(numMessagesToDispatch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void invokeAndTime (Runnable r) {
        int numRuns = 10;
        double seconds[] = new double[numRuns];
        for (int i = 0; i <numRuns; i++) {
            System.out.println("Starting run #" + i);
            long start = System.currentTimeMillis();
            r.run();
            double millis = (double)(System.currentTimeMillis()-start);
            seconds[i] = millis / 1000.0;
            System.out.println("Completed run #" + i + " in " + numberFormat.format(seconds[i]) + "s");
        }

        BigDecimal totalNumMessages = new BigDecimal(numMessagesToDispatch * numRuns);
        BigDecimal totalSecond = new BigDecimal(Arrays.stream(seconds).sum());
        BigDecimal avgMessagesPerSecond = totalNumMessages.divide(totalSecond, BigDecimal.ROUND_UP);
        System.out.println("    This took " + numberFormat.format(totalSecond)
                + "s for " + numberFormat.format(numRuns)
                + " runs, i.e. we could dispatch " + numberFormat.format(avgMessagesPerSecond) + " msg/s");
    }

    private void dispatchUsingListener(int numMessagesToDispatch) throws Exception {
        CountDownLatch cdl = new CountDownLatch(numMessagesToDispatch);
        Consumer<Integer> consumer = integer -> cdl.countDown();

        for (int i = 0; i < numMessagesToDispatch; i++) {
            consumer.accept(i);
        }

        cdl.await(20, TimeUnit.SECONDS);
        if (cdl.getCount()>0) throw new RuntimeException("Not all messages read");
    }

    private void dispatchUsingPublishSubject(int numMessagesToDispatch) throws Exception {
        CountDownLatch cdl = new CountDownLatch(numMessagesToDispatch);
        PublishSubject<Integer> publishSubject = PublishSubject.create();
        publishSubject.serialize().observeOn(Schedulers.io()).subscribe(integer -> cdl.countDown());

        for (int i = 0; i < numMessagesToDispatch; i++) {
            publishSubject.onNext(i);
        }
        publishSubject.onComplete();

        cdl.await(20, TimeUnit.SECONDS);
        if (cdl.getCount()>0) throw new RuntimeException("Not all messages read");
    }

    private void dispatchUsingBackpressuredStream(int numMessagesToDispatch) throws Exception {
        CountDownLatch cdl = new CountDownLatch(numMessagesToDispatch);
        BackpressuredStreamFromAsyncSource<Integer> stream = new BackpressuredStreamFromAsyncSource<>();
        stream.toFlowable().subscribeOn(Schedulers.io()).subscribe(integer -> cdl.countDown());

        for (int i = 0; i < numMessagesToDispatch; i++) {
            stream.onNext(i);
        }

        cdl.await(20, TimeUnit.SECONDS);
        stream.onComplete();
        if (cdl.getCount()>0) throw new RuntimeException("Not all messages read");
    }

    private void dispatchUsingBackpressuredStream2(int numMessagesToDispatch) throws Exception {
        CountDownLatch cdl = new CountDownLatch(numMessagesToDispatch);
        BlockingQueue<Integer> stream = new DisruptorBlockingQueue<Integer>(1);

        Thread reader = new Thread(() -> {
            int count = 0;
            while (count<numMessagesToDispatch) {
                try {
                    stream.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cdl.countDown();
                count++;
                if (count%10000 == 0) System.out.println(count);
            }
        });

        Thread writer = new Thread(() -> {
            for (int i = 0; i < numMessagesToDispatch; i++) {
                try {
                    stream.put(i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        reader.start();
        writer.start();
        stream.clear();

        cdl.await(100, TimeUnit.SECONDS);

        reader.join();
        writer.join();
        if (cdl.getCount()>0) throw new RuntimeException("Not all messages read");
    }

    private void dispatchUsingArrayBlockingQueue(int numMessagesToDispatch) throws Exception {
        CountDownLatch cdl = new CountDownLatch(numMessagesToDispatch);
        BlockingQueue<Integer> stream = new ArrayBlockingQueue<Integer>(1);

        Thread reader = new Thread(() -> {
            int count = 0;
            while (count<numMessagesToDispatch) {
                try {
                    stream.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cdl.countDown();
                count++;
            }
        });
        reader.start();

        for (int i = 0; i < numMessagesToDispatch; i++) {
            stream.put(i);
        }

        cdl.await(20, TimeUnit.SECONDS);
        if (cdl.getCount()>0) throw new RuntimeException("Not all messages read");
    }

    public static void main(String[] args) throws Exception {
        new BackpressuredStreamPerformance().go();
    }
}
