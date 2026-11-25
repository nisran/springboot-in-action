package org.esmile.concurrent.aqs.countdownlatch;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 业务场景：旅行预订聚合。一次行程要并发查 3 个站点——机票、酒店、租车，
 * 用异步 HTTP 客户端发出去（发完请求线程就走了），响应回来时跑在 IO 回调线程上。
 * 组装给用户的完整行程前，必须等 3 个回调都完成。
 *
 * 本例的核心点：countDown() 是从【回调线程】调用的，和发起请求的线程毫无关系。
 * CountDownLatch 没有持有者的概念——数到 0 就开门，谁数的都行：
 * 工作线程、IO 回调线程、定时器、另一个不相干的服务都可以。
 *
 * 为什么不用 Thread.join()？
 *   1. join 需要 Thread 对象的引用——线程池/异步客户端里你根本拿不到（手里只有 future）；
 *   2. 更致命：池化线程执行完任务【不会退出】，join 等的是"线程死亡"，
 *      对池线程 join 可能永远等下去（等到池 shutdown 为止）。
 *   join 只适合"我自己 new 的裸线程"这种最原始的场景。
 *
 * await() 也不阻塞发起方攒 future 的过程：先把 3 个请求全发出去（并发），
 * 再 await 收拢——这就是 fork-join 汇合点的 fork 阶段和 join 阶段。
 */
public class AsyncCallbackWaitDemo {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch allBack = new CountDownLatch(3);
        Queue<String> itinerary = new ConcurrentLinkedQueue<>();

        // fork 阶段：3 个请求同时发出，互不等待
        queryFlight().whenComplete((resp, err) -> collect("机票", resp, err, itinerary, allBack));
        queryHotel().whenComplete((resp, err) -> collect("酒店", resp, err, itinerary, allBack));
        queryCar().whenComplete((resp, err) -> collect("租车", resp, err, itinerary, allBack));

        // join 阶段：收拢，等 3 个回调各数一次数
        allBack.await();
        System.out.println("---- 3 个站点全部回来（count=0），组装行程 ----");
        itinerary.forEach(r -> System.out.println("  " + r));
    }

    /** 回调统一处理：结果入队 + 计数（注意运行线程既不是 main 也不是发请求的线程） */
    private static void collect(String site, String resp, Throwable err,
                                Queue<String> itinerary, CountDownLatch latch) {
        String thread = Thread.currentThread().getName();
        if (err != null) {
            latch.countDown(); // 失败也要数，别让主线程死等
            System.out.printf("[%s] ✗ %s 查询失败: %s（countDown 后 count=%d）%n",
                    thread, site, err.getMessage(), latch.getCount());
        } else {
            itinerary.add(resp);
            latch.countDown();
            System.out.printf("[%s] %s ✓（IO 回调线程数的数，countDown 后 count=%d）%n",
                    thread, resp, latch.getCount());
        }
    }

    /** 模拟异步 HTTP：响应在不同时刻从 IO 线程回来 */
    private static CompletableFuture<String> queryFlight() {
        return CompletableFuture.supplyAsync(() -> "机票 CA1831 ¥1200",
                CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS));
    }

    private static CompletableFuture<String> queryHotel() {
        return CompletableFuture.supplyAsync(() -> "酒店 杭州西湖希尔顿 ¥680",
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));
    }

    private static CompletableFuture<String> queryCar() {
        return CompletableFuture.supplyAsync(() -> "租车 Model 3 ¥350/天",
                CompletableFuture.delayedExecutor(700, TimeUnit.MILLISECONDS));
    }
}
