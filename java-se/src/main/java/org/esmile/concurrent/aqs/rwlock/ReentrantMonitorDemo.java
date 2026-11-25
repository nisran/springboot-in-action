package org.esmile.concurrent.aqs.rwlock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 业务场景：运维排查。生产上读写锁常包着核心数据结构，出问题得会看锁的内部状态。
 * 本例过一遍：两种锁各自的可重入、"写锁内拿读锁"（降级第一步）、监控指标。
 * <p>
 * 一句话记住 state 布局：一个 int 劈两半 —— 高 16 位读、低 16 位写。
 * 读计数 = 所有线程读锁持有次数【之和】（所以上限 65535，读远到不了）；
 * 写计数 = 写锁的重入次数（和 ReentrantLock 一个语义，见第 1 课 pay→deduct）。
 * <p>
 * 两个最容易混的监控指标（差一个词）：
 * getReadLockCount() ：全局视角，所有线程加起来的读持有次数（≈ state 高 16 位）；
 * getReadHoldCount() ：当前线程自己持有的读锁次数（每线程一份，存 ThreadLocal）。
 * <p>
 * 可重入规则汇总：
 * 写→写 ✓ 重入（低 16 位 +1）      读→读 ✓ 重入（自己的 HoldCounter +1）
 * 写→读 ✓ 允许（锁降级的前提）     读→写 ✗ 永远失败（锁升级死锁，见 LockUpgradeDeadlockDemo）
 */
public class ReentrantMonitorDemo {

    static final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    public static void main(String[] args) throws InterruptedException {
        // ① 写锁可重入：发布新版本（写）→ 里面再调一个写方法
        rw.writeLock().lock();
        rw.writeLock().lock();
        System.out.println("① 写锁重入 2 次: getWriteHoldCount=" + rw.getWriteHoldCount() // 2
                + ", isWriteLocked=" + rw.isWriteLocked());
        rw.writeLock().unlock();
        System.out.println("   释放 1 次: getWriteHoldCount=" + rw.getWriteHoldCount() // 1，还没真释放
                + "（减到 0 才真释放）");
        rw.writeLock().unlock();

        // ② 写锁内拿读锁：降级的第一步（同线程允许，state 变成 高16=1 低16=1）
        rw.writeLock().lock();
        rw.readLock().lock();
        System.out.println("② 持写锁期间拿读锁 ✓: getReadLockCount=" + rw.getReadLockCount() // 1
                + ", getWriteHoldCount=" + rw.getWriteHoldCount()); // 1
        rw.writeLock().unlock();
        System.out.println("   释放写锁后仍持读锁 ✓（降级完成，此刻只挡写不挡读）: "
                + "isWriteLocked=" + rw.isWriteLocked() + ", getReadLockCount=" + rw.getReadLockCount());
        rw.readLock().unlock();

        // ③ 全局计数 vs 本线程计数：main 持 2 次读锁，再拉一个线程持 1 次
        rw.readLock().lock();
        rw.readLock().lock(); // 读锁可重入
        Thread t = new Thread(() -> {
            rw.readLock().lock();
            System.out.println("③ [别的线程] 我也拿到读锁了: 全局 getReadLockCount=" + rw.getReadLockCount() // 3=main的2+我的1
                    + ", 我自己的 getReadHoldCount=" + rw.getReadHoldCount()); // 1
            rw.readLock().unlock();
        });
        t.start();
        t.join();
        System.out.println("   [main] 那个线程走了: 全局 getReadLockCount=" + rw.getReadLockCount() // 回到 2
                + ", main 自己的 getReadHoldCount=" + rw.getReadHoldCount()); // 2
        rw.readLock().unlock();
        rw.readLock().unlock();
        System.out.println("   全部释放: getReadLockCount=" + rw.getReadLockCount() + "（state 归零）");
    }
}
