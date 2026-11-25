package org.esmile.concurrent.lock;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 死锁 demo —— 对应 interview 02-concurrency/11-并发挑战 §04「检测与排查」
 *
 * 三幕：
 *  1. 元凶：t1/t2 反序加锁互等（synchronized，100% 必死）
 *  2. 连坐：每秒进来一个"转账请求"，都要先拿 A 锁——t1 永远不放手，
 *     请求线程只借不还 → JVM 线程总数单调上涨、锯齿消失（"只涨不落"）
 *  3. 保持现场：打印 pid + 排查命令，进程挂住，供 jstack / arthas attach
 *
 * 练手（第三幕打印的现场命令）：
 *   jstack <pid> | grep -A 20 "Found one Java-level deadlock"
 *   arthas:  dashboard / thread -b / thread --state BLOCKED
 */
public class DeadLock {

    static final Object LOCK_A = new Object();   // 想象成账户 A
    static final Object LOCK_B = new Object();   // 想象成账户 B

    static final AtomicInteger REQUEST_NO = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        long pid = ProcessHandle.current().pid();

        // ── 第一幕：元凶线程，反序加锁 ─────────────────────────────
        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {
                log("拿到 A，接下来要 B ...");
                sleep(500);                       // 保证 t2 此刻已拿到 B，制造互等
                synchronized (LOCK_B) {
                    log("拿到 B（永远走不到）");
                }
            }
        }, "deadlock-t1");

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {
                log("拿到 B，接下来要 A ...");
                sleep(500);
                synchronized (LOCK_A) {
                    log("拿到 A（永远走不到）");
                }
            }
        }, "deadlock-t2");

        t1.start();
        t2.start();
        t1.join(1000);                            // 1s 后双双挂死，join 超时返回

        // ── 第二幕：连坐线程 —— 模拟线上流量 ──────────────────────
        // cachedThreadPool：前面的线程卡死不归还，每个新请求都得起一个新线程
        // （有界池版本见类尾注释：表现是 active 顶格 + 队列堆积，而不是线程数涨）
        ExecutorService requestPool = Executors.newCachedThreadPool(r ->
                new Thread(r, "request-" + REQUEST_NO.incrementAndGet()));

        int lastThreads = ManagementFactory.getThreadMXBean().getThreadCount();
        for (int i = 1; i <= 20; i++) {
            requestPool.submit(() -> {
                synchronized (LOCK_A) {           // t1 永远不放手 → 请求全部 BLOCKED
                    log("拿到 A（永远走不到）");
                }
            });
            sleep(1000);
            int now = ManagementFactory.getThreadMXBean().getThreadCount();
            log("第 " + i + " 个请求进来：JVM 线程数 " + lastThreads + " → " + now
                    + (now > lastThreads ? "   ← 只涨不落（等锁的线程不归还）" : ""));
            lastThreads = now;
        }

        // ── 第三幕：保持现场，等工具 attach ────────────────────────
        System.out.println();
        System.out.println("=== 死锁已成型，现场保留，开始练手 ===");
        System.out.println("pid = " + pid);
        System.out.println("  jstack " + pid + " | grep -A 20 'Found one Java-level deadlock'");
        System.out.println("  jcmd  " + pid + " Thread.print | grep -c '^\"'    # 数线程数");
        System.out.println("  ps -o nlwp= -p " + pid + "                        # OS 线程数");
        System.out.println("  arthas: dashboard / thread -b / thread --state BLOCKED");
        System.out.println("（Ctrl+C 退出）");
        while (true) {
            sleep(60_000);                        // 主线程也挂住，现场不销毁
        }
    }

    // 修复版：锁排序 —— 全局约定永远先锁 id 小的，破坏"循环等待"（预防三板斧最常用的一把）
    static void safeTransfer(Account from, Account to) {
        Account first  = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            synchronized (second) {
                // doTransfer(from, to);
            }
        }
    }

    static class Account {
        final long id;
        Account(long id) { this.id = id; }
    }

    static void log(String msg) {
        System.out.printf("[%-12s] %s%n", Thread.currentThread().getName(), msg);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

/*
 * 有界池版本的连坐表现（把上面 cachedThreadPool 换成下面这行）：
 *   ExecutorService requestPool = Executors.newFixedThreadPool(200);
 * 现象变化：线程数涨到 200 后"顶格一条直线"不再涨，任务在无界队列里持续堆积，
 * 最终表现为拒绝/超时——两种池子对应监控曲线上两种形态，代码各 20 行就能复现。
 */
