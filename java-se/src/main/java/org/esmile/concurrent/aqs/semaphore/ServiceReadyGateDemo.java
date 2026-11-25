package org.esmile.concurrent.aqs.semaphore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 业务场景：微服务启动闸门。K8s 的 readiness 探针放流量之前，
 * 三件事必须全部就绪：拉配置中心、预热本地缓存、启动 MQ 消费者。
 * 三件事并行初始化，主线程等它们全部完成再"开闸"对外服务。
 *
 * Semaphore(0) 反向用法：初始 0 个许可，谁也 acquire 不到 —— 主线程先卡住；
 * 每个组件就绪后 release() 一次（permits: 0→1→2→3，就是 AQS 的 state 在涨），
 * 主线程 acquire(3) 一次性收齐 3 个"就绪事件"。本质是把 Semaphore 当事件计数器攒数。
 *
 * 与 CountDownLatch 的区别：latch 倒数到 0 就废了，一次性；
 * Semaphore 可重复关闸/开闸 —— 比如"每夜停机维护"：同一把闸门再 acquire(N) 关上，
 * 等 N 个维护任务 release 后重新放行。CountDownLatch 得每次 new 一个。
 *
 * 失败处理：组件初始化抛异常【也必须 release】，否则主线程永远卡死在闸门上；
 * 用失败标记让主线程收齐后立刻判定启动失败、进程退出，把容器交给 K8s 重启。
 */
public class ServiceReadyGateDemo {

    public static void main(String[] args) throws InterruptedException {
        Semaphore readyGate = new Semaphore(0); // 启动闸门：0 个许可，等 3 个组件各 release 一次
        AtomicBoolean failed = new AtomicBoolean(false);

        ExecutorService bootstrap = Executors.newFixedThreadPool(3);
        bootstrap.submit(() -> init("配置中心", 800, readyGate, failed));  // 拉配置快
        bootstrap.submit(() -> init("缓存预热", 1200, readyGate, failed)); // 预热慢
        bootstrap.submit(() -> init("MQ消费者", 300, readyGate, failed));  // 模拟失败
        bootstrap.shutdown(); // 注意不能等任务结束（close/awaitTermination），否则看不出闸门卡住的效果

        System.out.println("main: 等待 3 个组件就绪（acquire(3)，当前 permits=" + readyGate.availablePermits() + "）...");
        readyGate.acquire(3); // 一次性收齐 3 个许可，少一个都过不去
        System.out.println("main: 闸门放行，收齐 3 个就绪事件");

        if (failed.get()) {
            System.out.println("main: 有组件初始化失败 → 置为 NOT READY，进程退出交给 K8s 重启");
        } else {
            System.out.println("main: 全部就绪 → readiness 探针置为 UP，开放流量");
        }
    }

    /** 组件初始化：成功/失败都必须 release，否则主线程死等 */
    private static void init(String name, long costMs, Semaphore gate, AtomicBoolean failed) {
        String thread = Thread.currentThread().getName();
        try {
            Thread.sleep(costMs); // 模拟初始化耗时
            if ("MQ消费者".equals(name)) {
                throw new IllegalStateException("broker 连接失败");
            }
            System.out.printf("[%s] %s 就绪 ✓ (+1 permit)%n", thread, name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            failed.set(true);
            System.out.printf("✗ %s 初始化失败: %s（仍 release，让主线程能出来做决定）%n", name, e.getMessage());
        } finally {
            gate.release(); // 不管成败都放行一个事件 —— 关键：别让闸门死锁
        }
    }
}
