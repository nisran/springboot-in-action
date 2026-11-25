package org.esmile.desing.pattern.singleton;

// 饿汉式：类加载时即创建，天生线程安全
public class SingletonV5 {

    private static final SingletonV5 instance = new SingletonV5();

    private SingletonV5() {
    }

    public static SingletonV5 getInstance() {
        return instance;
    }
}
