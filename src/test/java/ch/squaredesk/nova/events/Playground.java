package ch.squaredesk.nova.events;

import com.sun.media.jfxmediaimpl.MediaDisposer;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;

public class Playground {
    private Emitter<String> emitter;
    private void go2() throws Throwable {
        PublishSubject<String> subject = PublishSubject.create();
        Observable<String> o = subject
                .doOnDispose(() -> System.err.println("Disposing"))
                .publish()
                .refCount();

        subject.onNext("1");
        System.out.println("subscribing 1");
        subject.onNext("2");
        Disposable d1 = o.subscribe(s -> System.out.println("  [1]: " + s));
        subject.onNext("3");
        System.out.println("subscribing 2");
        subject.onNext("4");
        Disposable d2 = o.subscribe(s -> System.out.println("  [2]: " + s));
        subject.onNext("5");
        System.out.println("disposing 1");
        subject.onNext("6");
        d1.dispose();
        subject.onNext("7");
        System.out.println("disposing 2");
        subject.onNext("8");
        d2.dispose();
        subject.onNext("9");
    }
    private void go3() throws Throwable {
        Observable<String> source = Observable.create(s -> {
            System.err.println("Creating connection");
            emitter = s;
            s.setDisposable(new Disposable() {
                @Override
                public void dispose() {
                    System.err.println("Killing connection");
                }

                @Override
                public boolean isDisposed() {
                    return false;
                }
            });
        });

        Observable<String> o = source.publish().refCount();

        Disposable d1 = o.subscribe(s -> {
            if ("b1".equals(s))
                throw new RuntimeException("Booom 1");
            else
                System.out.println("  [s1]: " + s);
        }, t -> System.out.println("  [s1]: " + t.getMessage()));

        System.out.println("Subscribed s1");
        Disposable d2 = o
                .map( s -> s.toUpperCase())
                .subscribe(s -> {
                    if ("b2".equalsIgnoreCase(s))
                        throw new RuntimeException("Booom 2");
                    else
                        System.out.println("  [s2]: " + s);
        }, t -> System.out.println("  [s2]: " + t.getMessage()));
        System.out.println("Subscribed s2");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        System.out.print("> "); line = reader.readLine();
        while (!("bye".equals(line))) {
            if ("s1".equals(line)) {
                d1.dispose();
            } else if ("s2".equals(line)) {
                d2.dispose();
            } else if ("disposeSource".equals(line)) {
                emitter.onComplete();
            } else {
                emitter.onNext(line);
            }
            System.out.print("> "); line = reader.readLine();
        }
        reader.close();
    }

    private void go() throws Throwable {
        PublishSubject<String> subject = PublishSubject.create();

        Flowable<String> flowable = subject.toFlowable(BackpressureStrategy.BUFFER).doFinally(() -> {
            System.out.println("Flowable finally");
            if (!subject.hasObservers()) {
                System.out.println("No observers left, nuking subject");
                subject.onComplete();
            }
        });

        Disposable flowableDisposable = flowable.subscribe(
                s -> System.out.println("got " + s),
                t -> {},
                () -> System.out.println("Finished"));


        Disposable triggerDisposable = Observable
                .interval(100,100, TimeUnit.MILLISECONDS)
                .doFinally(() -> {
                    System.err.println("triggerEmitter finished");
                    flowableDisposable.dispose();
                })
                .subscribe(
                        counter -> {
                            if ((counter+1)%23 == 0) subject.onError(new RuntimeException("Kabooom!"));
                            else subject.onNext(String.valueOf(counter));
                        },
                        error -> System.err.println("An error occurred on interval invocation for intervalId " ),
                        () -> {
                        });

        Thread.sleep(5000);
        triggerDisposable.dispose();

        System.out.println("hasSubscribers? " + subject.hasObservers());

        subject.onNext("Three");
    }

    public static void main(String[] args) throws Throwable {
        new Playground().go();
    }
}
