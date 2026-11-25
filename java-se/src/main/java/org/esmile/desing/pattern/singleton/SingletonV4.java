package org.esmile.desing.pattern.singleton;

public class SingletonV4 {

    // private constructor
    private SingletonV4() {
    }

    // 静态内部类：外部类加载时它不加载（懒加载）；
    // 首次访问 Holder.INSTANCE 时，JVM 用类初始化锁保证 <clinit> 只被执行一次（线程安全）。
    // 热路径上零锁、零 volatile，JIT 直接内联成一条 getstatic —— 实测与裸读同价。
    private static class Holder {
        private static final SingletonV4 INSTANCE = new SingletonV4();
    }

    public static SingletonV4 getInstance() {
        return Holder.INSTANCE;
    }
}
