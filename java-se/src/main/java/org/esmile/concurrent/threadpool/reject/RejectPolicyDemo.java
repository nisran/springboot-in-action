package org.esmile.concurrent.threadpool.reject;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 直接用 JDK 自带的 ThreadPoolExecutor，逐个演示四种内置拒绝策略 + 各自的使用场景。
 * <p>
 * 统一配置：core=2, max=3, workQueue 容量=2
 * 总容量 = core(2) + queue(2) + (max-core)(1) = 5，一口气提交 8 个 → 第 6 个起触发拒绝
 */
public class RejectPolicyDemo {

    public static void main(String[] args) throws InterruptedException {
        abort();
        callerRuns();
        discard();
        discardOldest();
    }

    /* ---------- ① AbortPolicy（默认）----------
     * 行为：抛 RejectedExecutionException，快速失败。
     * 场景：任务不能丢的关键业务（下单、支付、扣库存）。异常把决策权交回调用方：
     *      catch 之后自行降级——同步执行 / 重试 / 落库等补偿 job 处理。
     *      JDK 拿它当默认值的原因：绝不掩盖问题，过载要让人第一时间看见。 */
    static void abort() throws InterruptedException {
        run("AbortPolicy（默认）", new ThreadPoolExecutor.AbortPolicy());
    }

    /* ---------- ② CallerRunsPolicy ----------
     * 行为：谁提交谁执行，任务直接在【调用者线程】里 run()，天然背压——提交方被占用，提交速率自动降下来。
     * 场景：任务轻量、允许提交方变慢——本地缓存刷新、日志批量 flush、低优先级批处理。
     * 两个坑：
     *   a) 调用者若是 Tomcat 工作线程，慢任务会拖住整个上游请求，慢 IO/RPC 场景慎用；
     *   b) 池已 shutdown 时任务被【静默丢弃】（JDK 源码：if (!e.isShutdown()) r.run()，
     *      else 什么都不做），连异常都没有——优雅关停期间提交的任务会漏掉。 */
    static void callerRuns() throws InterruptedException {
        run("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /* ---------- ③ DiscardPolicy ----------
     * 行为：静默丢弃新任务，不抛异常、不通知任何人。
     * 场景：可丢的旁路任务——metrics 上报、日志埋点、推荐位补充内容，新值覆盖旧值无所谓。
     * 风险：丢了完全无感知，必须配「丢弃量」监控；重要业务禁用。 */
    static void discard() throws InterruptedException {
        run("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy());
    }

    /* ---------- ④ DiscardOldestPolicy ----------
     * 行为：丢掉队首【最老】的任务腾位置，再把新任务入队——牺牲旧任务保新任务。
     * 场景：新鲜度优先——行情快照、监控采样、UI 状态刷新，新的比老的值钱。
     * 坑：若 workQueue 是 PriorityBlockingQueue，队首是优先级【最高】的任务，
     *     「丢最老」实际变成「丢最重要」——这个组合是经典反直觉 bug。 */
    static void discardOldest() throws InterruptedException {
        run("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    // ================= 公共跑法 =================

    static void run(String name, RejectedExecutionHandler handler) throws InterruptedException {
        System.out.printf("%n===== %s =====%n", name);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 3, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                named("biz-worker"),
                handler);

        for (int i = 1; i <= 8; i++) {
            final int id = i;
            try {
                pool.execute(() -> {
                    System.out.printf("  [%s] 执行任务 %d%n", Thread.currentThread().getName(), id);
                    sleep(300);
                });
            } catch (RejectedExecutionException e) {
                System.out.println("  提交任务 " + id + " 被拒（调用方 catch 到，可自行降级）");
            }
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        // 注意 completedTaskCount 只统计池内线程跑的任务：
        //   Abort / Discard / DiscardOldest 稳定为 5
        //   CallerRuns = 8 减去 main 亲跑的个数（main 被占多久决定它代跑几个，时序相关）——
        //   调用者线程跑的任务不进池统计，监控口径要留意这种偏差
        System.out.println("  → 池内实际完成 " + pool.getCompletedTaskCount() + " / 8");
    }

    static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger(1);
        return r -> new Thread(r, prefix + "-" + n.getAndIncrement());
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
