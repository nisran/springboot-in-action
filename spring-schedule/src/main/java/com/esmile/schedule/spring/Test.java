package com.esmile.schedule.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;

@Slf4j
// @Component
public class Test {

    //cron 表达式
    @Scheduled(cron = "0/30 * * * * * ")
    public void testCron() {
        log.info("cron任务, 线程：{} <====> 时间：{}",Thread.currentThread().getName(), new Date());
    }

    //
    @Scheduled(fixedDelay = 9000)
    public void testDelay() throws InterruptedException {
        // log.info("线程：{} <====> 时间：{}",Thread.currentThread().getName(), new Date());
        Thread.sleep(1000);
        log.info("Delay任务, 线程：{} <====> 时间：{}",Thread.currentThread().getName(), new Date());
    }

    @Scheduled(fixedRate = 20000)
    public void testRate() throws InterruptedException {
        log.info("Rate任务, 线程：{} <====> 时间：{}",Thread.currentThread().getName(), new Date());
    }

}
