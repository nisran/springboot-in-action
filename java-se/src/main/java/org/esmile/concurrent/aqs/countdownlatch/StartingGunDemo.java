package org.esmile.concurrent.aqs.countdownlatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务场景：比价系统要"同一时刻"抓 5 家供应商的报价 —— 只有同时抓到的价格才有可比性，
 * 差两秒可能就错过一个促销。5 个抓取线程各自准备（建连/登录，快慢不一），
 * 准备好的卡在发令枪上；main 确认全部就位后开枪，5 个线程同一瞬间冲出去。
 *
 * 发令枪模式：new CountDownLatch(1)，count=1。
 *   所有 worker await() → state=1 不放行，全体 park 在队列里；
 *   main countDown() 一次 → state 1→0 → 共享模式把等待队列【全部】唤醒。
 * 一方发令、多方同时起跑 —— 和"多方等发令"正好是 latch 的两种基本用法。
 *
 * 本例用了两把 latch 配合，正好把两种用法串起来：
 *   ready(5)：攒"就位"事件，main 等它们（多方 → 一方等）；
 *   gun(1)：发令枪，它们等 main（一方 → 多方同时跑）。
 *
 * 细节：await 返回后还要等 CPU 调度，"同时"是毫秒级对齐而非纳秒级；
 * latch 打开后永远开着，后来的线程 await() 直接通过（可当"启动完成开关"用）。
 */
public class StartingGunDemo {

    public static void main(String[] args) throws InterruptedException {
        int suppliers = 5;
        CountDownLatch ready = new CountDownLatch(suppliers); // 就位计数
        CountDownLatch gun = new CountDownLatch(1);           // 发令枪
        AtomicLong firedAt = new AtomicLong();                // 记开枪时刻，算起跑偏移

        ExecutorService pool = Executors.newFixedThreadPool(suppliers);
        for (int i = 1; i <= suppliers; i++) {
            int id = i;
            pool.submit(() -> {
                try {
                    Thread.sleep(200 + (long) (Math.random() * 600)); // 建连/登录，各家快慢不同
                    ready.countDown();
                    System.out.printf("供应商-%d 就位（已到 %d/%d 家，等发令）%n",
                            id, suppliers - ready.getCount(), suppliers);
                    gun.await(); // 卡在这里，等枪响
                    long offsetMs = (System.nanoTime() - firedAt.get()) / 1_000_000;
                    Thread.sleep(30 + (long) (Math.random() * 70)); // 抓报价
                    System.out.printf("供应商-%d 报价到手 ¥%d（起跑偏移 +%dms）%n",
                            id, 4000 + id * 100, offsetMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await(); // 第一把 latch：等 5 家全部就位
        System.out.println("---- 全部就位，开枪！（countDown 1→0，全体同时唤醒）----");
        firedAt.set(System.nanoTime());
        gun.countDown(); // 第二把 latch：一声令下
        pool.shutdown();
    }
}
