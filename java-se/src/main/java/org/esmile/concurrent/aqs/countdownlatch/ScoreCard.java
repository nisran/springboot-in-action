package org.esmile.concurrent.aqs.countdownlatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ScoreCard {
    public static void main() throws InterruptedException {
        int num = 3;
        CountDownLatch latch = new CountDownLatch(num);
        List<CompletableFuture<Integer>> cfList = new ArrayList<>(num);
        for (int i = 0; i < 3; i++) {
            CompletableFuture<Integer> cf1 = CompletableFuture.supplyAsync(() -> {
                latch.countDown();
                return 100;
            }, CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));
            cfList.add(cf1);
        }

        latch.await();
        CompletableFuture.allOf(cfList.toArray(new CompletableFuture[0])).join();

        List<Integer> scores = cfList.stream().map(CompletableFuture::join).toList();
        System.out.println("done scores: " + scores);

    }
}
