// package com.esmile.jdkTimer;
//
// import java.util.Date;
// import java.util.Timer;
// import java.util.TimerTask;
//
// public class JDKTimer {
//
//     public static void main(String[] args) {
//         //定时任务本身
//         TimerTask timerTask = null;
//
//         //线程调度任务的调用工具
//         Timer timer = new Timer();
//         // timer.schedule(() -> System.out.println("hello"), 100);
//         timer.schedule(new TimerTask() {
//             @Override
//             public void run() {
//                 System.out.println("hello");
//             }
//         }, new Date(), 1000);
//     }
// }
