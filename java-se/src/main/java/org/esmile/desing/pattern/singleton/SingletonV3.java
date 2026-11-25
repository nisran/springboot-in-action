package org.esmile.desing.pattern.singleton;

/**
 * 双重检查锁（DCL）单例：锁外快速判空 + 锁内二次判空。
 * <p>
 * ==================== 为什么 instance 必须加 volatile？ ====================
 * <p>
 * instance = new SingletonV3() 不是一步，实际分三步：
 * ① 分配内存
 * ② 执行构造器，初始化对象
 * ③ 把内存地址赋给 instance
 * <p>
 * 若不加 volatile，②③ 可能被 JIT/CPU 重排成 ①③②（对锁内线程不可见）。
 * 而锁外的快速路径 if (instance != null) 与锁内写入之间没有任何同步关系，
 * 别的线程就可能看到"非 null、但构造器还没跑完"的半成品引用，
 * 拿到一个字段全是默认值的残缺对象。
 * <p>
 * ==================== 加了 volatile 会怎么样？ ====================
 * <p>
 * - 写侧：禁止 ②③ 重排，且保证构造器里写入的所有字段，
 * 在 instance 变得对外可见之前全部就位（release 语义）
 * - 读侧：每次读 instance 都拿到最新值，不会用寄存器/缓存里的旧值（acquire 语义）
 * - 代价：读操作从普通读变成 volatile 读，略慢一点点，
 * 但仍远好于 V2 每次抢锁的写法
 * <p>
 * 注意：volatile 只负责封"半成品"漏洞，替代不了 synchronized——
 * 多个线程仍可能同时通过外层判空，只有锁内的第二次判空能保证只 new 一次。
 * 两者缺一不可：volatile 管可见性/有序性，synchronized 管互斥。
 */
public class SingletonV3 {

    // volatile：禁止 new 的指令重排，保证别的线程要么看到 null，要么看到完整成品
    private static volatile SingletonV3 instance;

    // private constructor
    private SingletonV3() {
    }

    public static SingletonV3 getInstance() {
        if (instance != null) {          // 第一次检查：无锁快速路径，初始化完成后零开销
            return instance;
        }
        synchronized (SingletonV3.class) {
            if (instance == null) {      // 第二次检查：防止排队的线程重复创建
                instance = new SingletonV3();
            }
        }
        return instance;
    }
}
