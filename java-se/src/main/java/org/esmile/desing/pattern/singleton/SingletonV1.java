package org.esmile.desing.pattern.singleton;

public class SingletonV1 {
    private static SingletonV1 instance;

    // private constructor
    private SingletonV1() {
    }

    public static SingletonV1 getInstance() {
        if (instance == null) {
            instance = new SingletonV1();
        }
        return instance;
    }
}
