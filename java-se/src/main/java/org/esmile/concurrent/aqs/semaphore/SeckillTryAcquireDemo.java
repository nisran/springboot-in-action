package org.esmile.concurrent.aqs.semaphore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务场景：秒杀。100 个请求抢 10 台手机。
 *
 * 核心选型：无参 tryAcquire() —— 非阻塞，抢不到【立刻】返回"已售罄"。
 * 反面教材：写成 acquire() 阻塞等待的话，90 个抢不到的请求各占一个 Tomcat 线程干等，
 * 200 个线程的池子瞬间被拖满，正常商品的下单也全被堵死 —— 一次秒杀把整个服务打雪崩。
 * "快速失败 + 挡在门外"是高并发保护的第一原则。
 *
 * permits 就是库存：秒杀里只减不还，退款时 release() 回补（库存还能加回去）。
 * 由此能看出 Semaphore 的本质是【计数器】而非锁：release 甚至能把 permits 加到
 * 超过初始值（它没有"持有者"概念，所以也被叫作"没有所有者的锁"）。
 *
 * new Semaphore(10, false) 非公平模式：新来的线程先"闯"一把 CAS 再老老实实排队，
 * 吞吐优先 —— 秒杀要的就是这个；true = 严格 FIFO（内部批处理调度用，无饥饿但吞吐低）。
 */
public class SeckillTryAcquireDemo {

    /** 库存 = 10 个许可；非公平：抢购就别排队 */
    private final Semaphore stock = new Semaphore(10, false);
    private final AtomicInteger sold = new AtomicInteger();
    private final AtomicInteger soldOut = new AtomicInteger();

    public boolean buy(int userId) {
        if (stock.tryAcquire()) {           // 非阻塞：一次 CAS(state-1)，失败马上返回 false
            sold.incrementAndGet();
            System.out.printf("用户-%3d ✓ 抢到（剩余 %d）%n", userId, stock.availablePermits());
            return true;
        }
        soldOut.incrementAndGet();
        return false;                       // 立刻返回"已售罄"，线程毫秒级释放
    }

    /** 退款回补库存：release 一次，permit +1，后面的抢购又能买到 */
    public void refund() {
        stock.release();
        System.out.println("退款回补库存 +1，当前库存 " + stock.availablePermits());
    }

    public static void main(String[] args) throws InterruptedException {
        SeckillTryAcquireDemo seckill = new SeckillTryAcquireDemo();
        long start = System.nanoTime();

        try (ExecutorService app = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int userId = 1; userId <= 100; userId++) {
                int id = userId;
                app.submit(() -> {
                    try {
                        Thread.sleep((long) (Math.random() * 30)); // 模拟点击时间打散
                    } catch (InterruptedException ignored) {
                    }
                    seckill.buy(id);
                });
            }
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("---- %d 个请求 %dms 内全部返回：成交 %d，售罄 %d ----%n",
                100, elapsedMs, seckill.sold.get(), seckill.soldOut.get());

        // 退款回补：2 个等待名单里的用户补单成功
        seckill.refund();
        seckill.refund();
        System.out.println("补单成交 " + (seckill.buy(101) && seckill.buy(102) ? "2" : "?") + " 单，最终库存 "
                + seckill.stock.availablePermits());
    }
}
