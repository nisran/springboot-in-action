package com.esmile.jvm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CPU 高演示：复现面试笔记 03-jvm「3-7 调优排查」CPU 高四类原因的第一类——
 * 死循环 / 低效算法，练习 top -Hp → jstack 连抓 3 次的定位动线。
 *
 * 两种指纹一次备齐：
 * 1. /cpu/start  死循环   —— 专属线程 cpu-burn-demo，RUNNABLE 且行号纹丝不动；
 * 2. /cpu/slow   低效算法 —— 跑在 Tomcat 请求线程上，行号在小范围跳动但出不了同一方法。
 *
 * 演示动线：Bruno 发 /cpu/start → top -Hp 找最耗 CPU 的 tid → printf "%x" 转十六进制
 * → jstack 连抓 3 次（间隔 5s）grep nid=0x… → 行号相同即死循环实锤 → /cpu/stop 灭火。
 *
 * @Description CPU 高演示端点（死循环/低效算法指纹复现）
 * @Date 11/27/2026
 * @Created by nisran
 */
@Slf4j
@RestController
@RequestMapping("/cpu")
public class CpuDemoController {

    /** 灭火开关：volatile 保证 stop 请求线程的写入对烧 CPU 线程立即可见 */
    static volatile boolean BURNING = false;

    /** 烧 CPU 线程的迭代计数：既证明它真在干活，也让 JIT 无法消除循环体 */
    static final AtomicLong STEPS = new AtomicLong();

    private volatile Thread burner;

    /** 点火：起一条专属线程跑 while(BURNING) 死循环，独占一个核直到 /cpu/stop */
    @GetMapping("/start")
    public String start() {
        final Thread current = burner;
        if (current != null && current.isAlive()) {
            return "已在烧了，线程 " + current.getName() + " 还活着，累计 " + STEPS.get() + " 步";
        }
        STEPS.set(0);
        BURNING = true;
        final Thread t = new Thread(this::burn, "cpu-burn-demo");
        t.setDaemon(true);
        burner = t;
        t.start();
        log.info("死循环已点火，线程名 cpu-burn-demo，用 top -Hp 找它");
        return "已点火：线程名 cpu-burn-demo。接下来 top -Hp 找最耗 CPU 的 tid，"
                + "jstack 连抓 3 次对比行号；灭火调 /cpu/stop";
    }

    /** 灭火：置 false，烧 CPU 线程在下一圈检测到开关后退出 */
    @GetMapping("/stop")
    public String stop() {
        if (!BURNING) {
            return "没在烧。累计步数 " + STEPS.get();
        }
        BURNING = false;
        return "已灭火，共烧了 " + STEPS.get() + " 步。jstack 里 cpu-burn-demo 会变成 TERMINATED 消失";
    }

    /** 低效算法：O(n²) 冒泡排序，跑在 Tomcat 请求线程上，几十秒后自动返回 */
    @GetMapping("/slow")
    public String slow(@RequestParam(defaultValue = "100000") int n) {
        final int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = ThreadLocalRandom.current().nextInt();
        }
        log.info("低效算法开始：冒泡排序 {} 个元素，请求线程会被占住几十秒", n);
        final long begin = System.currentTimeMillis();
        bubbleSort(a);
        final long costMs = System.currentTimeMillis() - begin;
        log.info("低效算法结束：耗时 {}ms", costMs);
        return String.format("冒泡排序 %d 个元素耗时 %dms（请求线程 %s）",
                n, costMs, Thread.currentThread().getName());
    }

    /** 故意不用 Arrays.sort：O(n²) 才能让 jstack 三次快照都停在这一小段代码里 */
    private static void bubbleSort(int[] a) {
        final int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (a[j] > a[j + 1]) {
                    final int tmp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = tmp;
                }
            }
        }
    }

    /** 死循环体：模乘迭代是循环携带依赖，JIT 消不掉；volatile 读每圈都要做 */
    private void burn() {
        long x = 1;
        while (BURNING) {
            x = (x * 31 + 7) % 1_000_000_007L;
            STEPS.incrementAndGet();
        }
        log.info("死循环已退出，最终 x={}，累计 {} 步", x, STEPS.get());
    }
}
