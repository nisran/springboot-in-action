package org.esmile.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AQS：AbstractQueuedSynchronizer
 * 1. private volatile int state
 * 2. Queue：
 * private transient volatile Node head;
 * private transient volatile Node tail;
 */
public class AQS {
    static void main() {
        // AbstractQueuedSynchronizer
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        System.out.println();
        lock.unlock();
    }

    ConcurrentHashMap<Long, ReentrantLock> stockLocks = new ConcurrentHashMap<>();

    public boolean deductStock(Long skuId, int count) {
        ReentrantLock lock = getLock(skuId);
        boolean locked = false;
        try {
            locked = lock.tryLock(200, TimeUnit.MILLISECONDS);
            if (!locked) {
                // 200ms 还没排上，说明这个 SKU 正在被高频争抢
                // 直接返回“稍后重试”，把线程还给线程池
                throw new RuntimeException("库存繁忙，请稍后重试");
            }
            // 查库存、校验、扣减、写 DB
            return doDeduct(skuId, count);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("请求被中断");
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private boolean doDeduct(Long skuId, int count) {
        //todo
        return true;
    }

    private ReentrantLock getLock(Long skuId) {
        ReentrantLock reentrantLock = stockLocks.get(skuId);
        if (reentrantLock != null) {
            return reentrantLock;
        } else {
            ReentrantLock lock = new ReentrantLock();
            stockLocks.put(skuId, lock);
            return lock;
        }
    }
}
