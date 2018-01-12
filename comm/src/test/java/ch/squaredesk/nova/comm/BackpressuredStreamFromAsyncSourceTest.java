package ch.squaredesk.nova.comm;

import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackpressuredStreamFromAsyncSourceTest {
    @Test
    void onNextThrowsIfStreamWasAlreadyCompleted() {
        BackpressuredStreamFromAsyncSource<String> sut = new BackpressuredStreamFromAsyncSource<>();
        sut.onComplete();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> sut.onNext("Hello"));
        assertThat(ex.getMessage(), is("Stream closed"));
    }

    @Test
    void onCompleteInvokesCloseAction() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);
        Runnable closeAction = () -> cdl.countDown();
        BackpressuredStreamFromAsyncSource<String> sut = new BackpressuredStreamFromAsyncSource<>(closeAction);

        sut.toFlowable().subscribeOn(Schedulers.io()).subscribe();
        sut.onComplete();

        cdl.await(1, TimeUnit.SECONDS);
        assertThat(cdl.getCount(), is(0L));
    }

}