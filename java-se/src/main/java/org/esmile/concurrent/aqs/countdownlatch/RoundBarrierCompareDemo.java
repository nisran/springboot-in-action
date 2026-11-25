package org.esmile.concurrent.aqs.countdownlatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 业务场景：回合制。3 个玩家打副本，每个回合所有人打完才能进下一回合——
 * "互相等"的场景。用它对照 CountDownLatch 最大的局限：一次性。
 *
 * CountDownLatch 的 state 只减不增，减到 0 就"焊死"了，永远 open：
 *   - 无法 reset，第二回合必须 new 一把新 latch；
 *   - 谁也不知道"这一回合什么时候开始"，每个回合都要提前建好、传给所有参与者。
 * CyclicBarrier 专为此而生：到齐 → 放行 → 【自动复位】等下一轮，一把用到底，
 * 还能带 barrierAction（每回合到齐时由最后到达者执行回调，比如回合结算）。
 *
 * 选型口诀：
 *   主线程等 N 个任务干完（单向汇合）     → CountDownLatch（可带超时）
 *   N 个线程互相等齐再一起走（回合制/分代）→ CyclicBarrier
 *   等齐之后还要"同时起跑"               → 回合制场景配两把 latch（见 StartingGunDemo）
 *   动态增减参与者 / 多阶段分段           → Phaser（下下课）
 */
public class RoundBarrierCompareDemo {

    public static void main(String[] args) {
        System.out.println("== 方案A：CountDownLatch，每个回合 new 一把（一次性，到 0 焊死）==");
        playRoundsWithLatch();
        sleep(1200); // 等方案A跑完再开方案B，避免输出混在一起

        System.out.println();
        System.out.println("== 方案B：CyclicBarrier，一把用到底（自动复位 + 回合结算回调）==");
        playRoundsWithBarrier();
    }

    /** 方案A：回合数固定，提前为每个回合建一把 latch，玩家打完 countDown 再 await */
    static void playRoundsWithLatch() {
        int players = 3, rounds = 2;
        CountDownLatch[] roundLatches = new CountDownLatch[rounds];
        for (int r = 0; r < rounds; r++) {
            roundLatches[r] = new CountDownLatch(players);
        }

        ExecutorService pool = Executors.newFixedThreadPool(players);
        for (int p = 1; p <= players; p++) {
            int id = p;
            pool.submit(() -> {
                try {
                    for (int r = 0; r < rounds; r++) {
                        fight(id, r + 1);
                        roundLatches[r].countDown(); // 我打完了
                        roundLatches[r].await();      // 等其他人也打完（到齐 state=0 全放行）
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        pool.shutdown();
    }

    /** 方案B：同一把 barrier 循环 await，到齐自动放行并复位 */
    static void playRoundsWithBarrier() {
        int players = 3, rounds = 2;
        CyclicBarrier barrier = new CyclicBarrier(players, () ->
                System.out.println("    >> CyclicBarrier：全员到齐，回合结算并自动复位，进入下一回合"));

        ExecutorService pool = Executors.newFixedThreadPool(players);
        for (int p = 1; p <= players; p++) {
            int id = p;
            pool.submit(() -> {
                try {
                    for (int r = 0; r < rounds; r++) {
                        fight(id, r + 1);
                        barrier.await(); // 同一把 barrier 复用，不用 new
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        pool.shutdown();
    }

    private static void fight(int playerId, int round) {
        sleep(100 + (long) (Math.random() * 300));
        System.out.println("    玩家-" + playerId + " 第 " + round + " 回合行动完毕");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
