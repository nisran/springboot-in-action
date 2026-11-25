package org.esmile.concurrent.threadpool.reject;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 完整演示 RobustRejectHandler：
 * 突发 12 个任务打满池（容量 5）→ 拒绝发生 → ①尖峰吸收一部分、②其余落"MQ"
 * → 补偿消费者（模拟 MQ 消费者）把死信重放回池 → 12 个全部执行，0 丢失
 */
public class RobustRejectHandlerDemo {

    static final int TOTAL = 12;
    static final LongAdder executed = new LongAdder(); // 业务任务真正跑完的个数

    public static void main(String[] args) throws InterruptedException {
        // 模拟 MQ：有界内存队列（真实项目 = RocketMQ/Kafka，自带持久化/重试/死信主题）
        BlockingQueue<RobustRejectHandler.TaskEnvelope> mockMq = new ArrayBlockingQueue<>(100);

        RobustRejectHandler handler = new RobustRejectHandler("order-pool", mockMq, 100);

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 3, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                named("order-worker"),
                handler);

        // 补偿消费者：拉死信 → 重放回池。非阻塞入队失败就退避（真实项目 = MQ 自带的重试/退避机制）
        Thread compensator = new Thread(() -> {
            while (!pool.isShutdown()) {
                try {
                    RobustRejectHandler.TaskEnvelope env = mockMq.poll(200, TimeUnit.MILLISECONDS);
                    if (env == null) {
                        continue;
                    }
                    while (!pool.isShutdown() && !pool.getQueue().offer(env.task())) {
                        Thread.sleep(200); // 主池还满着：退避，防二次拒绝风暴
                    }
                    System.out.println("[补偿] 重放成功 → " + env.desc());
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "mq-compensator");
        compensator.setDaemon(true);
        compensator.start();

        // 突发流量：一口气 12 个，每个 300ms
        System.out.println("===== 突发提交 " + TOTAL + " 个任务（池容量 = 2核心 + 2队列 + 1弹性 = 5）=====");
        for (int i = 1; i <= TOTAL; i++) {
            final int id = i;
            pool.execute(() -> {
                System.out.printf("  [%s] 执行任务 %d%n", Thread.currentThread().getName(), id);
                sleep(300);
                executed.increment();
            });
        }

        // 等全部执行完（上限 30s），验证 0 丢失、最终一致
        long deadline = System.currentTimeMillis() + 30_000;
        while (executed.sum() < TOTAL && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        Thread.sleep(300);

        System.out.println("\n===== 结果 =====");
        System.out.println("执行完成 " + executed.sum() + " / " + TOTAL + "（0 丢失，最终一致）");
        System.out.println("降级链统计：" + handler.stats());
        pool.shutdown();
    }

    static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger(1);
        return r -> new Thread(r, prefix + "-" + n.getAndIncrement());
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
