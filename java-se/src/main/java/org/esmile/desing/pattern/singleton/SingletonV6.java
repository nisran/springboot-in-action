package org.esmile.desing.pattern.singleton;

public enum SingletonV6 {

    INSTANCE;

    private static String name;
    private static Integer version;


    // getter
    public static String getName() {
        return name;
    }

    public static Integer getVersion() {
        return version;
    }
}

class test {
    static void main() {
        SingletonV6 instance = SingletonV6.INSTANCE;
    }
}