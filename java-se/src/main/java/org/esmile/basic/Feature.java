package org.esmile.basic;

public class Feature {
    static void main() throws InterruptedException {
        virtual();
        Thread.sleep(1000);


    }

    /**
     * 本质：少量线程挂在到OS线程，有JVM调度
     */
    private static void virtual() {
        Runnable run = () -> System.out.println("hello virtual thread");
        Thread.ofVirtual().name("v-", 0).start(run);
    }

    //
    private static void switchs() {
        int a = 0;
        switch (a) {
            case 0:
                System.out.println(0);
                break;
            case 1:
                System.out.println(1);
                break;
            default:
                System.out.println("?");
        }

        // 14
        int num = 1;
        switch (num) {
            case 0, 1, 2 -> System.out.println(2);
            case 3, 4, 5 -> System.out.println(5);
        }

        // 17
        Object o = new String("1");
        switch (o) {
            case String s -> System.out.println(s);
            case Integer i -> System.out.println(i);
            case Float f -> System.out.println(f);
            case Double d -> System.out.println(d);
            default -> System.out.println("?");
        }
    }
}
