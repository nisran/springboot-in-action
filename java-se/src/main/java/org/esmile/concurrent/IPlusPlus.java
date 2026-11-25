package org.esmile.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

public class IPlusPlus {
    //    static volatile int t = 0;
    static AtomicInteger t = new AtomicInteger(0);

    static void main() {
        Thread[] threads = new Thread[100];
        Runnable r = () -> {
            for (int i = 0; i < 1000; i++) {
                t.getAndIncrement();
            }
        };
        for (int i = 0; i < threads.length; i++) {
            Thread thread = new Thread(r);
            threads[i] = thread;
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
        System.out.println("result: %d".formatted(t.get()));
    }
}
