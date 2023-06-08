package co.cmatts.aws.v2.dynamo;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CollectingSubscriber<T> implements Subscriber<T> {
    private static final long TIMEOUT_MILLIS = 5000L;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final List<T> collectedItems = new ArrayList<>();
    private Throwable error = null;
    private boolean isCompleted = false;

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        collectedItems.add(t);
    }

    @Override
    public void onError(Throwable throwable) {
        this.error = throwable;
        this.latch.countDown();
    }

    @Override
    public void onComplete() {
        this.isCompleted = true;
        this.latch.countDown();
    }

    public void waitForCompletion() {
        try {
            this.latch.await(TIMEOUT_MILLIS, MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<T> collectedItems() {
        return collectedItems;
    }

    public Throwable error() {
        return error;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}