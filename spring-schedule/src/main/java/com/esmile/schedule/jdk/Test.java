// package com.esmile.jdkTimer;
//
// import lombok.SneakyThrows;
// import lombok.extern.slf4j.Slf4j;
// import org.assertj.core.util.DateUtil;
//
// import java.text.SimpleDateFormat;
// import java.util.Calendar;
// import java.util.Date;
// import java.util.Timer;
// import java.util.TimerTask;
// import java.util.concurrent.TimeUnit;
//
// /**
//  * Copyright (C), 1998-2021, Shenzhen Rambo Technology Co., Ltd
//  * JDK 自带调度任务
//  *
//  * 弊端：
//  * 任务调度都采用同一线程池同一线程
//  * 如果一旦某个任务响应时间过长，将会导致线程阻塞（单线程执行多个任务）
//  *
//  * @author  Rambo
//  * @date    2021/2/22 17:05
//  * @since   1.0.0.1
//  */
// @Slf4j
// public class Test {
//
//     // 定义任务调度器实例对象
//     private static final Timer timer = new Timer();
//
//     public static void main(String[] args) {
//         timer1();
//         timer2();
//         timer3();
//         timer4();
//     }
//
//     /**
//      * 方法一：
//      * schedule(TimerTask task, long delay, long period);
//      * 设定指定任务 task 在指定延迟 delay 后进行固定延迟 period 的执行
//      * 0ms之后开始执行，每隔 1000 ms执行一次
//      *
//      * @author  Rambo
//      * @date    2021/2/22 17:19
//      */
//     public static void timer1(){
//         timer.schedule(new TimerTask(){
//             @SneakyThrows
//             @Override
//             public void run(){
//                 log.info("-------------> 调度线程名称：[{}]，被调度方法名称：[TimerScheduleTasks.timer1()]，执行频率：1秒/次，当前时间：[{}]", Thread.currentThread().getName(), DateUtil.now());
//                 // 模拟业务处理时长
//                 TimeUnit.SECONDS.sleep(5);
//             }
//         }, 0 , 1000);
//     }
//
//     /**
//      * 方法二：
//      * schedule(TimerTask task, Date time);
//      * 设定指定任务 task 在指定时间 time 后执行一次
//      * 5ms 后执行一次 run 方法后结束，执行完成后线程处于挂起等待状态
//      *
//      * @author  Rambo
//      * @date    2021/2/22 17:19
//      */
//     public static void timer2(){
//         timer.schedule(new TimerTask(){
//             @SneakyThrows
//             @Override
//             public void run(){
//                 log.info("-------------> 调度线程名称：[{}]，被调度方法名称：[TimerScheduleTasks.timer2()]，当前时间：[{}]", Thread.currentThread().getName(), DateUtil.now());
//                 // 模拟业务处理时长
//                 TimeUnit.SECONDS.sleep(5);
//             }
//         }, 5000);
//     }
//
//     /**
//      * 方法三：
//      * scheduleAtFixedRate(TimerTask task, long delay, long period);
//      * 设定指定任务 task 在指定延迟 delay 后进行固定频率 period 的执行
//      * 0ms 后，每隔 2s 执行一次
//      * @author  Rambo
//      * @date    2021/2/22 17:39
//      */
//     public static void timer3() {
//         timer.scheduleAtFixedRate(new TimerTask() {
//             @SneakyThrows
//             @Override
//             public void run() {
//                 log.info("-------------> 调度线程名称：[{}]，被调度方法名称：[TimerScheduleTasks.timer3()]，执行频率：2秒/次，当前时间：[{}]", Thread.currentThread().getName(), DateUtil.now());
//                 // 模拟业务处理时长
//                 TimeUnit.SECONDS.sleep(5);
//             }
//         }, 0, 2000);
//     }
//
//     /**
//      * 方法四：
//      * scheduleAtFixedRate(TimerTask task, Date firstTime, long period);
//      * 设定指定的任务 task 在指定的时间 firstTime 开始进行重复的固定速率 period 执行
//      * 每天在预设时间去执行，执行完成后线程处于挂起等待状态
//      *
//      * @author  Rambo
//      * @date    2021/2/22 17:45
//      */
//     public static void timer4(){
//         Calendar calendar = Calendar.getInstance();
//         // 控制时
//         calendar.set(Calendar.HOUR_OF_DAY , 17);
//         // 控制分
//         calendar.set(Calendar.MINUTE , 51);
//         // 控制秒
//         calendar.set(Calendar.SECOND , 0);
//
//         Date time = calendar.getTime();
//         SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss");
//         log.info("任务计划执行时间：[{}]", format.format(time));
//
//         timer.scheduleAtFixedRate(new TimerTask(){
//             @SneakyThrows
//             @Override
//             public void run(){
//                 log.info("-------------> 调度线程名称：[{}]，被调度方法名称：[TimerScheduleTasks.timer4()]，执行频率：每天固定时间执行，当前时间：[{}]", Thread.currentThread().getName(), DateUtil.now());
//                 // 模拟业务处理时长
//                 TimeUnit.SECONDS.sleep(5);
//             }
//             // 这里设定将延时每天固定执行
//         }, time, 1000 * 60 * 60 * 24);
//     }
// }
