package org.esmile.concurrent.aqs.reentrant;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 扣库存：`stock < n` 和 `stock -= n` 是两步（check-then-act），不加锁会超卖。
 * 用 tryLock(timeout) 而不是死等 lock()：抢购场景排队超时就快速失败让用户重试，
 * 好过线程干等占着资源 —— 超时放弃这一点 synchronized 做不到。
 */
@Slf4j
public class StockDedunkDemo {

    private final ReentrantLock lock = new ReentrantLock();
    private int stock = 100;

    public boolean deduct(int count) {
        boolean locked = false;
        try {
            locked = lock.tryLock(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断标记，让上层有机会感知并退出
            return false;
        }
        if (!locked) {
            return false; // 排队超时，本轮失败
        }
        try {
            if (stock < count) {
                return false; // 库存判断必须在锁内，否则判断和扣减之间会被插队
            }
            stock -= count;
            return true;
        } finally {
            lock.unlock(); // 成败都要释放，所以放 finally
        }
    }

    public static void main(String[] args) throws InterruptedException {
        StockDedunkDemo seckill = new StockDedunkDemo();
        int buyers = 300;
        CountDownLatch done = new CountDownLatch(buyers);
        AtomicInteger sold = new AtomicInteger();

        for (int i = 0; i < buyers; i++) {
            new Thread(() -> {
                if (seckill.deduct(1)) {
                    sold.incrementAndGet();
                }
                done.countDown();
            }, "buyer-" + i).start();
        }
        done.await();

        // 不超卖的铁律：卖出的 + 剩的 == 初始库存
        log.info("sold = {}, left = {}, total = {}", sold.get(), seckill.stock, sold.get() + seckill.stock);
    }
}
