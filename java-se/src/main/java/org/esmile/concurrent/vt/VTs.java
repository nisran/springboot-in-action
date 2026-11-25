package org.esmile.concurrent.vt;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class VTs {
    public static void main(String[] args) {
        // ① 构建器：最灵活，可命名、可先不启动（unstarted）
        Thread vt = Thread.ofVirtual().name("vt-", 0).start(() -> System.out.println("vt start"));

        // ② 静态快捷方法：一行起一个
        Thread.startVirtualThread(() -> System.out.println("hello from " + Thread.currentThread()));

        // ③ 每任务一线程的 Executor：生产推荐写法（try-with-resources 自动等待收尾）
        var done = new AtomicInteger();                 // 计数，用来证明 100 万个任务真的全部跑完
        long start = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1_000_000; i++) {          // 百万任务 = 百万虚拟线程，平台线程池早已 OOM
                executor.submit(() -> {
                    done.incrementAndGet();             // 每个任务都运行在自己的虚拟线程里
                    return "Hi";
                });
            }
        }   // close() 隐式 awaitTermination：所有任务结束后才继续
        // 走到这里 done 一定是 1,000,000 —— 否则 close() 不会返回
        System.out.printf("done=%d, elapsed=%,d ms, caller isVirtual=%b%n",
                done.get(), (System.nanoTime() - start) / 1_000_000, Thread.currentThread().isVirtual());
    }
}
