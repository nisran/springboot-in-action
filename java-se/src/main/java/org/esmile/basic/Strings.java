package org.esmile.basic;

import java.util.Collections;
import java.util.List;

public class Strings {
    public static void main(String[] args) {
        System.out.println("好".length());
        System.out.println("😊".length());
        System.out.println("😊".charAt(1));

        // substring is new value
        System.out.println(String.valueOf(1).substring(0, 1));
//        LocalDate
        List<String> list = Collections.unmodifiableList(List.of("123"));
        //
        //

    }
}
