package org.esmile.concurrent.aqs.semaphore;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务场景：数据库连接池（HikariCP 的核心思想）。
 *
 * 连接是昂贵资源：建 TCP + 握手认证要几十毫秒，所以池子里只造 5 条，反复借还复用。
 * Semaphore 的 permits 就是"池里还剩几条空闲连接"，对应 AQS 的 state：
 *   借出 = tryAcquire()，state-1；不够就进 CLH 队列 park 排队；
 *   归还 = release()，state+1，并唤醒队头等待者。
 * 连接本身放 ConcurrentLinkedDeque，Semaphore 只负责计数和排队，不存数据。
 *
 * 真实连接池不会让请求无限等：等不到要快速失败（connectionTimeout），
 * 把"池已耗尽"的压力反馈给上游，而不是默默堆积请求。
 */
public class ConnectionPoolDemo {

    static class Connection {
        final int id;
        Connection(int id) { this.id = id; }
    }

    /** 空闲连接数（借出 -1，归还 +1） */
    private final Semaphore idlePermits;
    /** 空闲连接本体；permit 到手必然能从这里拿到连接，两者数量恒等 */
    private final ConcurrentLinkedDeque<Connection> idle = new ConcurrentLinkedDeque<>();

    public ConnectionPoolDemo(int poolSize) {
        this.idlePermits = new Semaphore(poolSize);
        for (int i = 1; i <= poolSize; i++) {
            idle.add(new Connection(i));
        }
    }

    /** 借连接：最多等 timeout 毫秒，等不到抛错（对应 connectionTimeout 超时） */
    public Connection borrow(long timeoutMs) throws InterruptedException {
        if (!idlePermits.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("获取连接超时：池已耗尽，等了 " + timeoutMs + "ms 放弃");
        }
        return idle.poll();
    }

    /** 归还：调用方必须 try/finally，忘了 release 连接就"漏"了，池子永久缩容 */
    public void giveBack(Connection conn) {
        idle.add(conn);
        idlePermits.release();
    }

    public static void main(String[] args) throws InterruptedException {
        ConnectionPoolDemo pool = new ConnectionPoolDemo(5);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger timeout = new AtomicInteger();

        // Java 21 虚拟线程：阻塞在 tryAcquire 上排队不占内核线程，正是适用场景
        try (ExecutorService app = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= 20; i++) {
                int taskId = i;
                app.submit(() -> {
                    long start = System.nanoTime();
                    try {
                        Connection conn = pool.borrow(1500);
                        long waitedMs = (System.nanoTime() - start) / 1_000_000;
                        System.out.printf("worker-%2d 拿到连接 #%d（等待 %3dms，当前剩余许可 %d，排队 %d 人）%n",
                                taskId, conn.id, waitedMs,
                                pool.idlePermits.availablePermits(), pool.idlePermits.getQueueLength());
                        Thread.sleep(400 + (long) (Math.random() * 500)); // 模拟执行 SQL
                        pool.giveBack(conn);
                        ok.incrementAndGet();
                    } catch (IllegalStateException e) {
                        System.out.printf("worker-%2d ✗ %s%n", taskId, e.getMessage());
                        timeout.incrementAndGet();
                    } catch (InterruptedException ignored) {
                    }
                });
            }
        } // close() 隐式 awaitTermination：等 20 个任务全部跑完

        System.out.println("成功 " + ok.get() + " 个，超时 " + timeout.get() + " 个，剩余许可 "
                + pool.idlePermits.availablePermits() + "（应回到 5）");
    }
}
