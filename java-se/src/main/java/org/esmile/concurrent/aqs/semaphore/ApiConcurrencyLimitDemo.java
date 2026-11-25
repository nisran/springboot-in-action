package org.esmile.concurrent.aqs.semaphore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务场景：调用第三方支付通道，合同规定"最多 3 个并发"，超了对方直接封 IP。
 * <p>
 * 所以发请求前先占一个并发名额（tryAcquire 带超时），
 * 拿不到名额不等死，快速降级：提示用户稍后重试（保护自己，也别打爆下游）。
 * <p>
 * 本例最关键的点：acquire 在【请求线程】，release 在【异步回调线程】。
 * HTTP 客户端是异步的——发完请求线程就走了，响应回来时是 IO 线程在处理回调，
 * 名额由回调线程归还。Semaphore 允许这样跨线程 release（它只是计数器，没有"持有者"），
 * 而 ReentrantLock 不行：非持有线程 unlock 会直接抛 IllegalMonitorStateException。
 * 这是 Semaphore 和锁的本质区别。
 */
public class ApiConcurrencyLimitDemo {

    /**
     * 并发名额：同时最多 3 个请求在途
     */
    private final Semaphore inFlight = new Semaphore(3);
    private final AtomicInteger degraded = new AtomicInteger();

    /**
     * 模拟异步调用第三方：请求发出后 800ms，响应在【延迟调度线程】上回来
     */
    private CompletableFuture<String> callChannelAsync(int orderId) {
        return CompletableFuture.supplyAsync(
                () -> "通道应答: order=" + orderId + " 支付受理成功",
                CompletableFuture.delayedExecutor(800, TimeUnit.MILLISECONDS));
    }

    /**
     * 下单：拿不到并发名额就快速降级，绝不等死。返回在途请求的 future，供上层等待
     */
    public CompletableFuture<Void> pay(int orderId) throws InterruptedException {
        if (!inFlight.tryAcquire(150, TimeUnit.MILLISECONDS)) {
            degraded.incrementAndGet();
            System.out.printf("order-%2d ✗ 降级：并发名额已满（排队 150ms 放弃），提示稍后重试%n", orderId);
            return CompletableFuture.completedFuture(null);
        }
        System.out.printf("order-%2d → 发起支付（请求线程 %s，在途名额剩 %d）%n", orderId, Thread.currentThread().getName(), inFlight.availablePermits());
        return callChannelAsync(orderId).whenComplete((resp, err) -> {
            // 这里已经不是发请求的线程了，而是 delayedExecutor 的调度线程
            System.out.printf("order-%2d ← %s（回调线程 %s，归还 1 个名额）%n",
                    orderId, resp != null ? resp : err, Thread.currentThread().getName());
            inFlight.release(); // 跨线程归还：acquire 与 release 不同线程
        }).thenApply(v -> null);
    }

    static void main(String[] args) throws InterruptedException {
        ApiConcurrencyLimitDemo gateway = new ApiConcurrencyLimitDemo();
        List<CompletableFuture<Void>> pending = new ArrayList<>();

        for (int orderId = 1; orderId <= 12; orderId++) {
            pending.add(gateway.pay(orderId));
            Thread.sleep(100); // 模拟流量以 100ms/单 的节奏打进来
        }

        System.out.println("---- 流量打完，等在途请求全部回来 ----");
        CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).join();
        System.out.println("降级 " + gateway.degraded.get() + " 单；并发名额恢复 = "
                + gateway.inFlight.availablePermits() + "（应回到 3）");
    }
}
