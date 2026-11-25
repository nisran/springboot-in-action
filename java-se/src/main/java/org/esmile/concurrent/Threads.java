package org.esmile.concurrent;


import java.util.concurrent.*;

public class Threads {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        //create threads：only thread.start in some ways
        // original
        new MyThread().start();

        // runable interface
        Runnable runnable = () -> System.out.println("Hello Runnable!");
        new Thread(runnable).start();

        // callable
        callable();

        // thread pool
        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.submit(() -> System.out.println("Hello Executor!"));
        pool.shutdown();

        // completable feature
        CompletableFuture.supplyAsync(() -> "Hello CompletableFuture!"
//                        , Executors.newFixedThreadPool(2)
                )
                .thenAccept(System.out::println);

    }

    private static void callable() throws InterruptedException, ExecutionException {
        Callable<String> callable = () -> "Hello Callable!";

        // way 1: with feature task
        FutureTask<String> futureTask = new FutureTask<>(callable);
        new Thread(futureTask).start();
        System.out.println("feature task: " + futureTask.get());

        // way 2: with executors
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        threadPool.submit(new FutureTask<>(callable));
        String res;
        try {
            res = futureTask.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        System.out.println("thread pool: " + res);
        threadPool.shutdown();
    }

}

class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Hello Thread!");
    }
}