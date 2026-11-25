package org.esmile.concurrent.aqs.reentrant;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 手写迷你连接池：一把 ReentrantLock 挂两个 Condition。
 * 两个条件队列分开才能"精准唤醒"——还连接只唤醒借连接的，反之亦然；
 * synchronized 只有一个等待集，notifyAll 只能把所有人全吵醒。
 */
@Slf4j
public class SimplePool {

    /**
     * 占位用，真实场景是 JDBC/Redis 连接
     */
    public record Connection(String id) {
    }

    private final ReentrantLock lock = new ReentrantLock(true); // 公平锁：按先来后到分配，不饿死，代价是吞吐
    private final Condition notEmpty = lock.newCondition();     // 借连接的人在这等
    private final Condition notFull = lock.newCondition();     // 还连接的人在这等
    private final Deque<Connection> idle = new ArrayDeque<>();
    private final int capacity;

    public SimplePool(int capacity) {
        this.capacity = capacity;
    }

    public Connection borrow() throws InterruptedException {
        lock.lockInterruptibly();     // 等锁期间可被中断：优雅停机时能取消等待，synchronized 办不到
        try {
            while (idle.isEmpty()) {
                notEmpty.await();     // 池空，睡到有人还为止
            }
            Connection c = idle.poll();
            notFull.signal();         // 腾出空位，唤醒一个"还连接被阻塞"的线程（若有）
            return c;
        } finally {
            lock.unlock();
        }
    }

    public void release(Connection c) {
        lock.lock();
        try {
            // 正常用法（先借后还）不会走到这；保留它才是完整的有界缓冲形态
            while (idle.size() == capacity) {
                notFull.awaitUninterruptibly(); // 池满等别人借走。还连接不该被中断打断，所以不用会抛异常的 await()
            }
            idle.offer(c);
            notEmpty.signal();        // 有连接了，精准唤醒一个等借的线程
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SimplePool pool = new SimplePool(2);
        pool.release(new Connection("c1"));
        pool.release(new Connection("c2"));

        Connection a = pool.borrow();
        Connection b = pool.borrow();
        log.info("borrowed both: {}, {}", a.id(), b.id());

        // 池已空：第 3 个借用者挂起在 notEmpty 上，直到 main 还回一个连接被精准唤醒
        Thread t = new Thread(() -> {
            try {
                log.info("borrowed in other thread: {}", pool.borrow().id());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "borrower-3");
        t.start();

        Thread.sleep(500); // 确保 t 已经挂起，再还连接才能演示 signal 唤醒它
        pool.release(a);
        t.join();
    }
}
