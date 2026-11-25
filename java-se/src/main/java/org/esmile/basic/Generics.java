package org.esmile.basic;

public class Generics {
    public static void main(String[] args) {

    }
//    static double sum(List<? extends Number> src) {
//        double s = 0;
//        for (Number n : src) s += n.doubleValue();   // ✅ 读出来保证是 Number
//         src.add(1);  src.add(2.0);  src.add(null 只除外)   // ❌ 一律编译错
//        return s;
//    }
    // PECP： Producer Extends Consumer Super

}
