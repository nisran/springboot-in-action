package org.esmile.basic;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class Dates {
    public static void main(String[] args) {
//        oldOnes();

        newAPI();

    }

    /**
     * 槽点：构造器，有setter 方法，线程不安全，
     */
    private static void oldOnes() {
        // actual year =  1970+2026, month = 9+1
        Date d = new Date(2026, 9, 17);
        System.out.println(d);
        d.setYear(2026);

        //calendar
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 17);
    }

    private static void newAPI() {
        // 所见即所得
        LocalDateTime date = LocalDateTime.of(2026, 9, 17, 23, 59, 59);
        long epochSecond = date.toEpochSecond(ZoneOffset.ofHours(8));
        System.out.println(date);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        zonedDateTime.getZone();
        System.out.println(zonedDateTime);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println(formatter.format(date));

        Instant now = Instant.now();
        System.out.println(now.getNano());
        System.out.println(now.toEpochMilli());
        System.out.println(System.currentTimeMillis());
        Duration.ofMillis(now.toEpochMilli());
    }
}
