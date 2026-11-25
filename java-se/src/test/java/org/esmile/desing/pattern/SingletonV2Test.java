package org.esmile.desing.pattern;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SingletonV2Test {
    @Test
    void testGetInstanceConcurrent() throws Exception {
        int threads = 100;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        ConcurrentHashMap<SingletonV4, Boolean> map = new ConcurrentHashMap<>();

        Runnable runnable = () -> {
            ready.countDown();
            try {
                start.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            map.put(SingletonV4.getInstance(), true);
        };

        ArrayList<Thread> threads1 = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(runnable);
            threads1.add(thread);
            thread.start();
        }

        ready.await();     // 等全员就位
        start.countDown(); // 开枪

        for (Thread t : threads1) {
            t.join(); // 等所有线程跑完，主线程再统计
        }

        assertEquals(1, map.size(), "map size should be 1, but is " + map.size());
    }
}
