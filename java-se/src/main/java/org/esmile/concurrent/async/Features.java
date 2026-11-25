package org.esmile.concurrent.async;

import java.util.concurrent.*;


public class Features {
    static void main() {
        future();
    }


    /**
     * Future 三大缺陷
     * 1. get一直阻塞
     * 2. 缺乏组合能力
     * 3. 异常处理不便，get 时需要捕获 ExecutionException
     */
    private static void future() {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<String> future = pool.submit(() -> "hello world");
        try {
            // get一直阻塞，直到任务完成
            String result = future.get();
            System.out.println(result);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 默认使用ForkJoinPool.commonPool()
     * 坑：commonPool建议用CPU密集型，IO密集型任务会占用池子
     * 守护线程：commonPool中的线程都是守护线程，不会阻塞JVM退出
     */
    private static void CompletableFuture() {
        //编排
        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "hello world");
        CompletableFuture<Void> cf2 = CompletableFuture.runAsync(() -> System.out.println("hello java"));

        CompletableFuture.allOf(cf1, cf2)
                .thenApply( t-> cf1.join() + cf2.join())
                .thenAcceptAsync(System.out::println);

        //异常处理
        CompletableFuture.supplyAsync(() -> "hello world")
                // 正常和异常都会走
                .handle((r, e) -> {
                    if (e != null) {
                        System.out.println(e.getMessage());
                    }
                    return r;
                })
                // 链上处理异常
                .exceptionally(e -> {
                    System.out.println(e.getMessage());
                    return null;
                })
                // 最后处理
                .whenComplete((r, e) -> {
                    if (e != null) {
                        System.out.println(e.getMessage());
                    }
                });

        // 超时控制
        CompletableFuture.supplyAsync(() -> "hello world")
                // 超时直接报错
                .orTimeout(1, TimeUnit.SECONDS)
                // 超时返回默认值
                .completeOnTimeout("default", 5, TimeUnit.SECONDS)
                .thenRunAsync(System.out::println)
                // 如果是runAsync，参数为null
                .completeOnTimeout(null, 5, TimeUnit.SECONDS);
    }
}
