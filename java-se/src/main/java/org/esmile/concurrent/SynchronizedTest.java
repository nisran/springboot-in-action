package org.esmile.concurrent;

/**
 * synchronized：内置锁，悲观锁
 */
public class SynchronizedTest {

    // 非静态方法，锁对象：实例锁：this
    public synchronized void m1(){
    }

    // 静态方法：锁对象：类本身：this
    public static synchronized void m2(){
    }

    void m3(){
        //锁可以是this，可以是类，或者其他的final对象
        synchronized (SynchronizedTest.class) {
            System.out.println();
        }
    }
}
