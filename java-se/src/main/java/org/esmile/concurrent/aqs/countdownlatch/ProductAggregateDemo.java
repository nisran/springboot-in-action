package org.esmile.concurrent.aqs.countdownlatch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 业务场景：电商商品详情页聚合。一个页面要拼 4 块数据——基本信息、库存、推荐、评论，
 * 并行调 4 个下游服务，回来一个拼一块，全部回来（或超时）才渲染返回。
 *
 * CountDownLatch 的 count 就是 AQS 的 state（共享模式）：
 *   new CountDownLatch(4) → state=4；
 *   每个服务回来 countDown() 一次 → state 4→3→2→1→0；
 *   减到 0 的瞬间唤醒队列里【所有】await 的线程（共享模式传播唤醒）。
 * 主线程 await(800ms)：评论服务慢就放弃等待 —— 页面降级，评论位渲染占位符。
 * 分布式第一原则：慢比挂更可怕，任何跨服务调用都必须有超时。
 *
 * 两个工程要点：
 *   1. countDown() 必须放 finally —— 任务抛异常也要把数减掉，否则主线程永远卡在 await；
 *   2. getCount() 随时可查"还差几个没回来"，是天然的监控埋点。
 *
 * 和上一课 ServiceReadyGateDemo 的 Semaphore(0)+acquire(3) 攒事件是同一思想，
 * CountDownLatch 就是这个模式的官方特化版：state 只减不增，减到 0 永久放行。
 */
public class ProductAggregateDemo {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        Map<String, String> page = new ConcurrentHashMap<>();

        ExecutorService pool = Executors.newFixedThreadPool(4);
        pool.submit(() -> fetch("基本信息", 200, "iPhone 17 Pro 钛金属", latch, page));
        pool.submit(() -> fetch("库存", 300, "深圳仓 12 台", latch, page));
        pool.submit(() -> fetch("推荐", 400, "手机壳 / 贴膜 / 快充头", latch, page));
        pool.submit(() -> fetch("评论", 1500, "好评率 98%", latch, page)); // 慢服务
        pool.shutdown(); // 不能 awaitTermination，否则主线程还没 await 就全回来了

        boolean allBack = latch.await(800, TimeUnit.MILLISECONDS);
        if (allBack) {
            System.out.println("4 个服务全部回来");
        } else {
            System.out.println("✗ 等了 800ms 还差 " + latch.getCount() + " 个（评论服务慢）→ 降级处理");
        }
        render(page);

        Thread.sleep(1000); // 请求其实已经返回了；这里等慢服务真回来，看它"迟到"的样子
        System.out.println("评论服务最终还是回来了（迟到的数据回填缓存，下次请求直接命中）: "
                + page.get("评论") + "，count=" + latch.getCount() + "（到 0 后 latch 永久打开）");
    }

    /** 调一个下游服务：回来（或失败）都必须 countDown */
    private static void fetch(String service, long costMs, String data,
                              CountDownLatch latch, Map<String, String> page) {
        boolean ok = false;
        try {
            Thread.sleep(costMs); // 模拟 RPC 耗时
            page.put(service, data);
            ok = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown(); // 关键：不管成败都减数，别让主线程死等
            System.out.printf("%-5s %s（countDown 后 count=%d）%n",
                    service, ok ? "✓ 回来" : "✗ 失败（也计一次数）", latch.getCount());
        }
    }

    private static void render(Map<String, String> page) {
        System.out.println("---- 渲染详情页 ----");
        for (String block : new String[]{"基本信息", "库存", "推荐", "评论"}) {
            System.out.println("  " + block + ": " + page.getOrDefault(block, "【加载中…（降级占位）】"));
        }
    }
}
