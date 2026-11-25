package org.esmile.concurrent.aqs.reentrant;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 缓存击穿：热点 key 过期瞬间，大量请求同时回源打垮 DB。
 * Caffeine 一次解决两件事：
 * 1) get() 同 key 并发回源自动合并 —— 内部就是"按 key 加锁 + 双重检查"，一个线程跑 loader，其余等它；
 * 2) refreshAfterWrite 到点先返旧值、后台异步刷新 —— 请求永远拿得到数据、回源永远只有一个线程。
 * （手写等价物：ConcurrentHashMap<Long, ReentrantLock> + tryLock + 双重检查 + 旧值降级）
 */
@Slf4j
public class CacheBreakdownDemo {

    /**
     * 时间故意用秒，方便 main 里肉眼观察；生产一般是分钟级
     */
    private final LoadingCache<Long, ProductDetail> cache = Caffeine.newBuilder()
            .maximumSize(10_000)                     // 容量上限 + W-TinyLFU 淘汰，缓存不会无限堆 key
            .expireAfterWrite(10, TimeUnit.SECONDS)  // TTL
            .refreshAfterWrite(3, TimeUnit.SECONDS)  // 过期前主动后台续期，旧值始终可用
            .build(this::loadFromDb);

    public ProductDetail getDetail(Long productId) {
        return cache.get(productId); // 未命中才调 loader；同 key 并发只放一个进去
    }

    private ProductDetail loadFromDb(Long productId) {
        log.info(">>> loading {} from db", productId);
        try {
            Thread.sleep(200); // 模拟慢查询
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new ProductDetail(productId, "from-db");
    }

    public static void main(String[] args) throws InterruptedException {
        CacheBreakdownDemo demo = new CacheBreakdownDemo();

        // 首次并发加载：只有 1 个线程回源，其余等它并共享结果
        for (int i = 0; i < 5; i++) {
            new Thread(() -> log.info("got {}", demo.getDetail(1L)), "req-" + i).start();
        }
        Thread.sleep(1000);

        // 3 秒后到达 refreshAfterWrite 阈值：调用方瞬间拿旧值，回源在后台线程异步进行
        Thread.sleep(2500);
        long start = System.nanoTime();
        log.info("refresh window got {} in {} ms", demo.getDetail(1L),
                (System.nanoTime() - start) / 1_000_000);
        Thread.sleep(1000); // 等后台刷新的日志打出来，注意那条日志的线程名不是调用方线程
    }
}
