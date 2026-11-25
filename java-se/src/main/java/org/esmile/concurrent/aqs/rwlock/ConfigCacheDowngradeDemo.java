package org.esmile.concurrent.aqs.rwlock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 业务场景：商品配置的本地缓存。读路径极热（每个下单请求都查），写极少（运营改配置）。
 * 读多写少正是 ReentrantReadWriteLock 的主场：读锁共享（读读共存）、写锁独占（读写/写写互斥）。
 *
 * AQS 只有一个 int state，读写锁把它劈成两半：
 *   高 16 位 = 读锁被持有的总次数（所有线程之和）；
 *   低 16 位 = 写锁的重入次数。
 *
 * 本例主角是【锁降级】：写完数据后还要"用"这份数据（组装响应/序列化，可能耗时）。
 *   错误写法：释放写锁 → 再拿读锁。中间有空窗——另一个写线程可能插进来改掉数据；
 *   正确写法：持写锁期间先拿读锁 → 再释放写锁 —— 换手不松手，全程至少握着一把锁，
 *   别的写线程进不来，而读锁共享、不挡其他读者。
 * 同线程"写锁里拿读锁"是允许的，这正是降级的前提。
 *
 * 缓存未命中走双重检查（double check）：并发 8 个请求同时 miss，
 * 写锁保证只有第 1 个真查库，后面的进去发现已被填好，直接用。
 */
public class ConfigCacheDowngradeDemo {

    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    private final Map<String, String> cache = new HashMap<>(); // 所有访问都在锁内
    private final AtomicInteger dbHits = new AtomicInteger();

    public String get(String key) throws InterruptedException {
        rw.readLock().lock();
        String v;
        try {
            v = cache.get(key);
        } finally {
            rw.readLock().unlock();
        }
        if (v != null) {
            // 命中：同样持读锁用数据（真实缓存读路径的标准姿势——读读共存，N 个请求并行）
            rw.readLock().lock();
            try {
                return useData(v);
            } finally {
                rw.readLock().unlock();
            }
        }

        // 未命中 → 拿写锁加载
        rw.writeLock().lock();
        try {
            v = cache.get(key); // double check：排队期间可能别的线程已经加载好了
            if (v == null) {
                v = loadFromDb(key);
                cache.put(key, v);
                System.out.printf("[%s] 查库加载 %s 入缓存（DB 命中 %d 次）%n",
                        Thread.currentThread().getName(), key, dbHits.get());
            }
            rw.readLock().lock(); // ★ 降级第一步：持写锁时先拿读锁（同线程允许）
        } finally {
            rw.writeLock().unlock(); // ★ 第二步：释放写锁，手里还握着读锁
        }
        try {
            return useData(v); // 用数据：此刻挡写、不挡读
        } finally {
            rw.readLock().unlock();
        }
    }

    /** 运营改配置：写锁独占，等所有读者退出后才能进 */
    public void update(String key, String value) {
        rw.writeLock().lock();
        try {
            cache.put(key, value);
            System.out.println("[运营] 已更新 " + key + " → " + value);
        } finally {
            rw.writeLock().unlock();
        }
    }

    private String loadFromDb(String key) throws InterruptedException {
        dbHits.incrementAndGet();
        Thread.sleep(200); // 模拟慢查询
        return key + " 的配置（来自DB）";
    }

    /**
     * 用数据：模拟组装响应 100ms。
     * 注意观察输出：第一波 miss 走【降级路径】的线程 getReadLockCount 恒为 1——
     * 降级持有的读锁会堵住后面排队的写线程，写线程进不来也就拿不到自己的读锁，互相串行；
     * 最后那波【命中路径】的读者互不阻塞，能叠到 4。降级是换手不松手，不是免费的并行。
     */
    private String useData(String v) throws InterruptedException {
        Thread.sleep(100);
        System.out.printf("[%s] 组装响应完毕（此刻并发读者数 getReadLockCount=%d，读读共存）%n",
                Thread.currentThread().getName(), rw.getReadLockCount());
        return v;
    }

    public static void main(String[] args) throws InterruptedException {
        ConfigCacheDowngradeDemo cache = new ConfigCacheDowngradeDemo();

        // 8 个请求同时查一个不存在的 key：应只查一次库
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int i = 1; i <= 8; i++) {
                pool.submit(() -> {
                    try {
                        cache.get("sku:1001");
                    } catch (InterruptedException ignored) {
                    }
                });
            }
        }
        System.out.println("DB 实际命中 " + cache.dbHits.get() + " 次（双重检查：8 个并发 miss 只查一次库）");

        Thread.sleep(300);
        cache.update("sku:1001", "sku:1001 新配置：价格 ¥9999");

        // 之后的读者立刻看到新值（写锁保证可见性：解锁先于下一次加锁）
        System.out.println("更新后读到: " + cache.get("sku:1001"));

        // 缓存已热，4 个读者并发命中：读读共存，getReadLockCount 能看到叠到 4
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            for (int i = 1; i <= 4; i++) {
                pool.submit(() -> {
                    try {
                        cache.get("sku:1001");
                    } catch (InterruptedException ignored) {
                    }
                });
            }
        }
    }
}
