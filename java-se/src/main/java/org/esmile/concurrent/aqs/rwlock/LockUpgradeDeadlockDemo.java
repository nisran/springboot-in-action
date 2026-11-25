package org.esmile.concurrent.aqs.rwlock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 业务场景：一个真实的缓存刷新 bug。需求是"读的时候发现缓存脏了，就顺便刷新"，
 * 开发者顺手写出：拿读锁 → 判断脏 → 拿写锁刷新。一跑就卡死。
 *
 * 为什么锁升级（持读锁去拿写锁）不被支持？读锁是共享的，同一时刻可能有 N 个读者。
 * 假如允许升级：读者 A 想升级，得等"读计数归零"，也就是等读者 B 放读锁；
 * 而 B 也在升级，也在等 A 放 —— 互相等，死锁。所以实现干脆不允许：
 * 持读锁的线程拿写锁永远拿不到。
 *
 * 本例三步：
 *   ① 单线程试升级：tryLock 超时失败（不用 lock()，否则 demo 真卡死给你看）；
 *   ② 两个读者同时升级：双双失败 —— 证明这是结构性的，不是运气问题；
 *   ③ 正确写法：先彻底放读锁 → 拿写锁 → 【重新 double check】（松手期间别人可能已刷过）
 *      → 改完降级衔接 ConfigCacheDowngradeDemo。
 */
public class LockUpgradeDeadlockDemo {

    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    private String cached = "旧数据";
    private boolean dirty = true;

    public static void main(String[] args) throws InterruptedException {
        LockUpgradeDeadlockDemo demo = new LockUpgradeDeadlockDemo();
        demo.singleThreadUpgradeFails();
        System.out.println();
        demo.twoThreadsUpgradeDeadlock();
        System.out.println();
        demo.correctRefresh();
        System.out.println("最终缓存值: " + demo.cached);
    }

    /** ① 就算只有我一个读者，升级也拿不到：自己的读锁还握着，写锁就要等"读计数=0" */
    void singleThreadUpgradeFails() throws InterruptedException {
        rw.readLock().lock();
        try {
            System.out.println("① 拿到读锁（就我一个读者），尝试直接升级写锁...");
            boolean ok = rw.writeLock().tryLock(1, TimeUnit.SECONDS);
            System.out.printf("   tryLock(1s) 结果: %s —— 卡满 1 秒也拿不到；若用 lock() 就永久卡死了%n",
                    ok ? "成功" : "✗ 失败");
        } finally {
            rw.readLock().unlock();
        }
    }

    /** ② 两个读者同时升级：A 等 B 放读锁，B 等 A 放 —— 互等的经典死锁结构 */
    void twoThreadsUpgradeDeadlock() throws InterruptedException {
        CountDownLatch bothHoldingRead = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);
        for (int i = 1; i <= 2; i++) {
            int id = i;
            new Thread(() -> {
                rw.readLock().lock();
                try {
                    bothHoldingRead.countDown();
                    bothHoldingRead.await(); // 确保两人都握着读锁再同时试
                    boolean ok = rw.writeLock().tryLock(1, TimeUnit.SECONDS);
                    System.out.printf("   读者-%d 升级结果: %s（我在等对方放读锁，对方也在等我）%n",
                            id, ok ? "成功" : "✗ 失败");
                } catch (InterruptedException ignored) {
                } finally {
                    rw.readLock().unlock();
                    done.countDown();
                }
            }, "读者-" + i).start();
        }
        System.out.println("② 两个读者各持读锁，同时尝试升级写锁...");
        done.await();
    }

    /** ③ 正确写法：先放读锁，再拿写锁，进去重新检查（松手期间可能被别人刷新过） */
    void correctRefresh() {
        System.out.println("③ 正确写法：放读锁 → 拿写锁 → double check → 刷新");
        rw.readLock().lock();
        boolean need;
        try {
            need = dirty; // ① 先彻底松手
        } finally {
            rw.readLock().unlock();
        }
        if (!need) {
            return;
        }
        rw.writeLock().lock(); // ② 再拿写锁
        try {
            if (dirty) {       // ③ double check：这一步不能省！
                cached = "新数据";
                dirty = false;
                System.out.println("   写锁内重新检查后刷新缓存 ✓ → " + cached);
            } else {
                System.out.println("   写锁内发现别人已刷新过，跳过（double check 的意义）");
            }
        } finally {
            rw.writeLock().unlock();
        }
    }
}
