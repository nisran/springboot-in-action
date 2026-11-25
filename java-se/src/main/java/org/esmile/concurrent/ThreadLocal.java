package org.esmile.concurrent;

public class ThreadLocal {
    static void main() {
        Thread t = Thread.currentThread();            // ① 拿当前线程
//        ThreadLocalMap map = t.threadLocals;
        new java.lang.ThreadLocal<>();
    }
}
