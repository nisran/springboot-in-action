package org.esmile.concurrent.aqs.reentrant;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 支付回调幂等处理 demo —— 分段锁（Striped Lock）版本
 * <p>
 * 旧写法 ConcurrentMap<String, ReentrantLock> 每单一把锁且不清理，map 随订单数无限增长；
 * 而"用完就删"又有竞态：B 刚 get 到旧锁、A 已 remove、C 新建新锁，B/C 各持各的锁互斥失效。
 * 所以这里改成固定数量的锁，内存 O(STRIPES)，与订单量无关。
 */
@Slf4j
public class PayCallback {

    /**
     * 锁的数量，取 2 的幂方便位运算；1024 足够把撞锁概率压到约 1/1024
     */
    private static final int STRIPES = 1024;

    private final ReentrantLock[] locks = new ReentrantLock[STRIPES];

    /**
     * 模拟订单支付状态；生产环境应落在 DB 唯一索引或 Redis SETNX+TTL 上
     */
    private final Set<String> paidOrders = ConcurrentHashMap.newKeySet();

    public PayCallback() {
        for (int i = 0; i < STRIPES; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public void handlePayCallback(String orderNo) {
        // 用 hash 把订单定位到固定的一把锁。floorMod 保证非负；
        // STRIPES 是 2 的幂时等价于 hash & (STRIPES - 1)，即 HashMap 的经典写法
        //
        // 两个不同 orderNo 会撞进同一把锁（同 HashMap 撞桶），但处理方式相反：
        // HashMap 撞桶后必须靠 equals 链区分出精确的 key —— 查错 key 就是数据错误；
        // 分段锁只要求"同一单 → 永远同一把锁"（同一字符串 hashCode 恒定，天然满足），
        // 不同的单撞在一起只是被多余地互斥（伪冲突）：锁多给了不出错，少给才出错。
        ReentrantLock lock = locks[Math.floorMod(orderNo.hashCode(), STRIPES)];
        // 排队等，而不是 tryLock 失败就丢弃：撞锁的可能是另一单，另一单丢了不能靠自己重试兜底。
        // 撞锁的两单在此串行化，只多付毫秒级等待；同单重试等到锁后再查幂等，也不会重复发货。
        // （tryLock+丢弃 只在"回调方保证重试"时才可用：微信/支付宝收不到成功应答会间隔递增重试
        //   十几次、持续几小时，丢弃等价于应答失败，属于用延迟换洪峰吞吐 —— 没这个前提就不能丢。）
        lock.lock();
        try {
            if (orderAlreadyPaid(orderNo)) {   // 幂等检查
                log.info("order {} already paid, ignore", orderNo);
                return;
            }
            markPaid(orderNo);
            deliverGoods(orderNo);
        } finally {
            lock.unlock();
        }
    }

    private boolean orderAlreadyPaid(String orderNo) {
        return paidOrders.contains(orderNo);
    }

    private void markPaid(String orderNo) {
        paidOrders.add(orderNo);
    }

    private void deliverGoods(String orderNo) {
        log.info("order {} goods delivered", orderNo);
    }

    public static void main(String[] args) throws InterruptedException {
        PayCallback callback = new PayCallback();
        String orderNo = "ORDER-1001";

        // 模拟支付平台对同一订单的并发重试回调：
        // 预期 1 个线程发货，其余排队进锁后被幂等检查拦下（already paid, ignore）
        for (int i = 0; i < 5; i++) {
            new Thread(() -> callback.handlePayCallback(orderNo), "callback-" + i).start();
        }

        Thread.sleep(1000);
    }
}
