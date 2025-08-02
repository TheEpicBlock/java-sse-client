package nl.theepicblock.sseclient.test.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Doesn't contain any actually good multithreading
 */
public class Channel<T> {
    private BlockingQueue<T> buffer = new LinkedBlockingQueue<>();

    public void push(T t) {
        buffer.add(t);
    }

    public T waitForNext() {
        try {
            return buffer.poll(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
