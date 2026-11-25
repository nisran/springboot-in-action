package org.esmile.desing.pattern.singleton;

public class SingletonV2 {
    private static SingletonV2 instance;

    // private constructor
    private SingletonV2() {
    }

    public synchronized static SingletonV2 getInstance() {
        if (instance == null) {
            instance = new SingletonV2();
        }
        return instance;
    }
}
