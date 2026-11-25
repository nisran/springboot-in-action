package org.esmile.concurrent.aqs.rwlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 业务场景：风控黑名单服务。读 QPS 巨大（每笔交易都要查+校验），写极少（运营偶尔封号）。
 * 用同一份逻辑对比两种锁的读吞吐：
 * <p>
 * ReentrantLock（互斥锁）：读和读也互斥 → 3 个读线程被强制串行，互相踩脚；
 * ReentrantReadWriteLock：读读共享 → 3 个读线程真正并行（多核上≈核数倍收益）。
 * <p>
 * 一个重要的反直觉经验（本例第一个版本就是翻车的）：
 * 临界区如果只有纳秒级的 map.get，读锁的开销（CAS 同一个 state 字 +
 * 每次维护 ThreadLocal 读计数）比省下的串行时间还多，读写锁反而【更慢】。
 * 读写锁的收益前提 = 锁内有真实工作量（校验/组装/IO）。所以本例在读锁里
 * 放了一段固定 CPU 工作模拟校验——真实业务的读路径几乎总有这类工作量。
 * <p>
 * 适用边界（面试常考）：读多写少 → 收益大；写频繁 → 反而更慢：
 * 写要等全部读者退出，唤醒开销 + state 维护比普通锁重，还可能写者饥饿。
 */
public class BlacklistThroughputDemo {

    /**
     * 锁内工作量：约 10 万次整数运算，模拟一次校验/组装（真实业务读路径的常态）
     */
    static final int WORK = 100_000;

    public static void main(String[] args) throws InterruptedException {
        run(new ReentrantLock(), null, 300);            // JIT 预热，不计成绩
        run(null, new ReentrantReadWriteLock(), 300);

        long plainQps = run(new ReentrantLock(), null, 1000);
        long rwQps = run(null, new ReentrantReadWriteLock(), 1000);

        System.out.printf("%n互斥锁 ReentrantLock        : %,10d 次检查/秒%n", plainQps);
        System.out.printf("读写锁 ReentrantReadWriteLock: %,10d 次检查/秒%n", rwQps);
        System.out.printf("读多写少 + 锁内有真实工作量，读写锁快 %.1f 倍（读读终于并行了）%n",
                rwQps / (double) plainQps);
    }

    /**
     * 跑 durationMs 毫秒：3 个读线程循环检查 + 1 个写线程每 100ms 封一个新号，返回总检查数
     */
    static long run(ReentrantLock plain, ReentrantReadWriteLock rw, long durationMs)
            throws InterruptedException {
        Set<String> blacklist = ConcurrentHashMap.newKeySet(); // 数据本身线程安全，锁才是被测对象
        blacklist.addAll(List.of("bad-1001", "bad-1002", "bad-1003"));
        AtomicLong reads = new AtomicLong();
        AtomicBoolean stop = new AtomicBoolean();
        List<Thread> all = new ArrayList<>();

        all.add(new Thread(() -> { // 写线程：运营封号
            int i = 0;
            while (!stop.get()) {
                if (plain != null) {
                    plain.lock();
                    try {
                        blacklist.add("bad-" + (2000 + i++));
                    } finally {
                        plain.unlock();
                    }
                } else {
                    rw.writeLock().lock();
                    try {
                        blacklist.add("bad-" + (2000 + i++));
                    } finally {
                        rw.writeLock().unlock();
                    }
                }
                sleep(100);
            }
        }, "运营-封号"));

        for (int r = 1; r <= 3; r++) { // 读线程：交易风控检查（两种锁做完全相同的工作）
            all.add(new Thread(() -> {
                long i = 0, sink = 0;
                while (!stop.get()) {
                    if (plain != null) {
                        plain.lock();
                        try {
                            sink = checkOnce(blacklist, i, sink);
                        } finally {
                            plain.unlock();
                        }
                    } else {
                        rw.readLock().lock();
                        try {
                            sink = checkOnce(blacklist, i, sink);
                        } finally {
                            rw.readLock().unlock();
                        }
                    }
                    reads.incrementAndGet();
                    i++;
                }
                if (sink == Long.MIN_VALUE) System.out.print(""); // 防 JIT 死代码消除
            }, "风控检查-" + r));
        }

        all.forEach(Thread::start);
        Thread.sleep(durationMs);
        stop.set(true);
        for (Thread t : all) {
            t.join(2000);
        }
        return reads.get();
    }

    /**
     * 锁内的工作：查黑名单 + 一段固定校验计算（两种锁下完全相同）
     */
    static long checkOnce(Set<String> blacklist, long i, long sink) {
        boolean hit = blacklist.contains("merchant-" + (i % 500));
        for (int j = 0; j < WORK; j++) {
            sink += j ^ (hit ? 1 : 0);
        }
        return sink;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
