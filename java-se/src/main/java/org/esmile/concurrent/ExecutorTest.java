package org.esmile.concurrent;

import java.util.concurrent.*;

public class ExecutorTest {

    void Executors() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
    }

    void ThreadPoolExecutor() {
        new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS
                , new ArrayBlockingQueue<>(10)
                , new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
