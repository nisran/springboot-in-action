package org.esmile.concurrent.aqs.reentrant;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 可重入的业务意义：每个方法自己加锁保证线程安全，组合调用时才不会自己把自己锁死。
 * <p>
 * 场景：用户钱包。deduct/refund/record 都是独立入口（会被单独调用），各自加锁；
 * pay 的业务要求"扣款 + 记流水"必须原子（不能钱扣了、流水没记），
 * 所以 pay 整体在锁内，再嵌套调 deduct/record —— 此时同一把锁被同一线程拿了两次。
 * AQS 里就是 state 1→2，unlock 每次减 1，减到 0 才真正释放。
 * 换成不可重入的锁，pay 调 deduct 那一刻就自锁死锁。
 */
public class ReentrantFeatureDemo {

    private final ReentrantLock lock = new ReentrantLock();
    private long balance = 1000;

    /**
     * 交易流水：对账线程也会读这个 list，写必须在锁内
     */
    private final List<String> transactions = new ArrayList<>();

    /**
     * 扣款：独立入口（退款冲正也复用它），所以自己也要加锁
     */
    public void deduct(long amount) {
        lock.lock(); // 被 pay 调进来时是第 2 次拿锁，state 1→2
        try {
            System.out.println("deduct 内持有数: " + lock.getHoldCount()); // 组合调用时 2，单独调用时 1
            if (balance < amount) {
                throw new IllegalStateException("余额不足");
            }
            balance -= amount;
        } finally {
            lock.unlock(); // 减 1，锁还没真正释放
        }
    }

    /**
     * 退款：加钱 + 记流水，同样是组合方法
     */
    public void refund(long amount) {
        lock.lock();
        try {
            balance += amount;
            record("refund +" + amount);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 记流水：独立入口，与对账线程并发，必须加锁
     */
    public void record(String entry) {
        lock.lock();
        try {
            transactions.add(entry);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 支付：扣款和记流水必须同生共死，整体在锁内，内部再嵌套调 deduct/record
     */
    public void pay(long amount) {
        lock.lock();
        try {
            deduct(amount);
            record("pay -" + amount);
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        ReentrantFeatureDemo wallet = new ReentrantFeatureDemo();
        wallet.pay(300);   // pay → deduct → record，两层嵌套加锁，无死锁
        wallet.refund(50); // refund → record，同样嵌套
        System.out.println("balance = " + wallet.balance + ", transactions = " + wallet.transactions);
    }
}
