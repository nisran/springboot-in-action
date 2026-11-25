package org.esmile.desing.pattern;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

class SingletonTest {
    @Test
    void testGetInstanceConcurrent() throws Exception {
        int threads = 100;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        ConcurrentHashMap<SingletonV1, Boolean> map = new ConcurrentHashMap<>();

        Runnable runnable = () -> {
            ready.countDown();
            try {
                start.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            map.put(SingletonV1.getInstance(), true);
        };

        ArrayList<Thread> threads1 = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(runnable);
            threads1.add(thread);
            thread.start();
        }

        // wait for all threads to be ready then start
        ready.await();
        // start all threads
        start.countDown();

        for (Thread t : threads1) {
            t.join(); // 等所有线程跑完，主线程再统计
        }


        assert map.size() == 1 : "map size should be 1， but is " + map.size();
    }
}