package org.esmile.concurrent.threadpool.reject;

import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/**
 * 生产级自定义拒绝策略：一条「分级降级链」，任务尽量不丢、且绝不静默丢。
 * <p>
 * 拒绝发生（queue 满 && 线程数 = max）
 * │
 * ├─ ⓪ 永远先做：日志带现场 + 打点计数（后续监控/告警规则的数据源）
 * ├─ ① 限时回塞原队列（默认 100ms）：吸收瞬时尖峰；等不到说明不是尖峰是真过载
 * ├─ ② 投递 MQ（demo 用内存死信队列模拟）→ 补偿消费者异步重放，最终一致
 * └─ ③ 兜底抛 RejectedExecutionException：连 MQ 都投不进去（MQ 挂了/满了）就快速失败
 * <p>
 * 设计要点（面试加分项）：
 * - rejectedExecution 跑在【提交任务的调用者线程】上，必须快：
 * 日志走异步 appender；打点用 LongAdder（写多读少，比 AtomicLong 省竞争）
 * - 落 MQ 的任务要带上下文打包成「信封」（提交时间等）；真实项目里必须【可序列化】——
 * 存参数/消息体而不是 Runnable 引用，消费端重建任务再执行
 * - ① 的限时回塞本质是温和背压：会拖住提交方最多 100ms，该值必须可配置
 * - 重放端要限流退避：主池还没恢复就猛灌，会造成二次拒绝风暴
 */
public class RobustRejectHandler implements RejectedExecutionHandler {

    /**
     * 任务信封：包一层上下文再进"MQ"。真实项目里这对应发出去的消息体
     */
    public record TaskEnvelope(Runnable task, long submitTime) {
        public String desc() {
            return task + " @t=" + submitTime;
        }
    }

    private final String poolName;
    private final long reOfferMillis;
    /**
     * 模拟 MQ 的死信通道（真实项目：RocketMQ/Kafka，自带持久化、重试、死信主题）
     */
    private final BlockingQueue<TaskEnvelope> deadLetter;

    // 打点：写多读少 → LongAdder
    private final LongAdder rejected = new LongAdder();      // 拒绝总次数
    private final LongAdder spikeAbsorbed = new LongAdder(); // 被①限时回塞救回来的
    private final LongAdder toDeadLetter = new LongAdder();  // 走②进 MQ 的
    private final LongAdder hardFailed = new LongAdder();    // 降级链全挂、最终抛异常的

    public RobustRejectHandler(String poolName, BlockingQueue<TaskEnvelope> deadLetter, long reOfferMillis) {
        this.poolName = poolName;
        this.deadLetter = deadLetter;
        this.reOfferMillis = reOfferMillis;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        rejected.increment();  // ⓪ 先打点，后面每一步都可能 return

        // ⓪ 日志带现场：队列多满、几个线程、是不是在关停
        //（真实项目：slf4j WARN + 异步 appender + Prometheus counter + 告警规则）
        System.out.printf("[%s] 拒绝 #%d | queue=%d, active=%d/%d, completed=%d%n",
                poolName, rejected.sum(), e.getQueue().size(),
                e.getPoolSize(), e.getMaximumPoolSize(), e.getCompletedTaskCount());

        // ① 限时回塞，吸收瞬时尖峰。池已 shutdown 则语义上就该拒绝，不再回塞
        if (!e.isShutdown()) {
            try {
                if (e.getQueue().offer(r, reOfferMillis, TimeUnit.MILLISECONDS)) {
                    spikeAbsorbed.increment();
                    return;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // 不吞中断：恢复标记，走下一步
            }
        }

        // ② 落 MQ，交给补偿消费者异步重放
        TaskEnvelope envelope = new TaskEnvelope(r, System.currentTimeMillis());
        if (deadLetter.offer(envelope)) {
            toDeadLetter.increment();
            return;
        }

        // ③ 兜底：快速失败，绝不静默丢
        hardFailed.increment();
        throw new RejectedExecutionException(poolName + " 降级链全部失败: " + envelope.desc());
    }

    /**
     * 监控口径快照
     */
    public String stats() {
        return "拒绝=" + rejected.sum() + "，尖峰吸收=" + spikeAbsorbed.sum()
                + "，落MQ=" + toDeadLetter.sum() + "，硬失败=" + hardFailed.sum();
    }
}
