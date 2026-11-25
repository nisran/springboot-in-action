package com.esmile.oom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * 内存泄漏演示：静态集合持有引用，配合 -XX:+HeapDumpOnOutOfMemoryError 练习 dump 分析。
 *
 * 泄漏模型对应面试笔记 06-内存泄漏 场景表第一条：
 * 长生命周期对象（static List，GC Root 可达）持有短生命周期对象（byte[]）引用，
 * 对象"在用着"但永远不再读它 —— GC 视为存活，老年代基线持续缓涨，最终 Java heap space。
 *
 * 用前先在 IDEA VM options 里配黑匣子参数（见 docs），建议 -Xmx256m 让 OOM 几秒内触发。
 *
 * @Description OOM/内存泄漏演示端点
 * @Date 11/25/2026
 * @Created by nisran
 */
@Slf4j
@RestController
@RequestMapping("/oom")
public class OomDemoController {

    /** 泄漏根：static 字段 → GC Root，直到进程退出前从不释放 */
    static final List<byte[]> LEAK = new ArrayList<>();

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();

    /** 触发泄漏：每轮分配 chunkMb 大小的块塞进静态集合，直到堆耗尽抛 OutOfMemoryError */
    @GetMapping("/heap")
    public String leak(@RequestParam(defaultValue = "8") int chunkMb,
                       @RequestParam(defaultValue = "100") long intervalMs) {
        log.info("开始泄漏：chunk={}MB，当前堆 heapUsed={}MB", chunkMb, usedHeapMb());
        long round = 0;
        while (true) {
            final byte[] chunk = new byte[chunkMb * 1024 * 1024];
            LEAK.add(chunk);
            round++;
            if (round % 5 == 0) {
                log.info("第{}轮：累计持有{}MB，堆已用{}MB（上限见 VM options -Xmx）",
                        round, LEAK.size() * chunkMb, usedHeapMb());
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "已中断，累计持有 " + LEAK.size() * chunkMb + "MB";
            }
        }
    }

    /** 只看现状，不触发分配 */
    @GetMapping("/heap/status")
    public String status() {
        return String.format("静态集合持有 %d 个块 / %dMB；堆已用 %dMB",
                LEAK.size(), LEAK.size() * 8, usedHeapMb());
    }

    /** 对症下药演示：断引用后这些块在下一次 GC 即被回收 */
    @PostMapping("/heap/clear")
    public String clear() {
        final int mb = LEAK.size() * 8;
        LEAK.clear();
        log.info("已断开引用 {}MB，下次 GC 回收", mb);
        return "已断开引用 " + mb + "MB，观察 gc.log 里老年代是否回落";
    }

    private static long usedHeapMb() {
        return MEMORY.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }
}
